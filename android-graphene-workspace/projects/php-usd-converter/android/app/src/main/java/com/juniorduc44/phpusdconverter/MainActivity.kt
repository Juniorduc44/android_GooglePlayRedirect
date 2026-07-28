package com.juniorduc44.phpusdconverter

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.juniorduc44.phpusdconverter.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    /** USD per 1 PHP */
    private var phpToUsdRate: Double = FALLBACK_RATE
    private var rateIsLive: Boolean = false
    /** true = Convert tab primary currency PHP */
    private var phpToUsd: Boolean = true
    /** true = Travel tab cost in PHP (independent of Convert) */
    private var travelPhp: Boolean = true
    /** true = distance in km */
    private var useKm: Boolean = true
    /** Weight input unit: lb | kg | g */
    private var weightUnit: String = "lb"
    /** true = input in Celsius (food / oven); false = Fahrenheit */
    private var tempCelsius: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versionLabel.text = "v${BuildConfig.VERSION_NAME}"
        binding.rateInfoLabel.text = getString(R.string.fetching_rate)
        applyDirectionLabels()
        applyTravelCurrencyLabels()
        applyDistanceUnitLabels()
        applyWeightUnitLabels()
        applyTempUnitLabels()

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_convert))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_travel))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_weight))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_temp))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_settings))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.tabFlipper.displayedChild = tab.position
                when (tab.position) {
                    1 -> calculateTravel()
                    2 -> calculateWeight()
                    3 -> calculateTemp()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.convertButton.setOnClickListener { convertCurrency() }
        binding.swapButton.setOnClickListener { swapDirection() }
        binding.amountEntry.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.amountEntry.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                convertCurrency(); true
            } else false
        }

        binding.unitSwitchButton.setOnClickListener { swapDistanceUnit() }
        binding.travelCurrencyButton.setOnClickListener { swapTravelCurrency() }
        binding.travelCalcButton.setOnClickListener { calculateTravel() }
        val travelWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { calculateTravel() }
        }
        binding.distanceEntry.addTextChangedListener(travelWatcher)
        binding.travelCostEntry.addTextChangedListener(travelWatcher)

        binding.weightUnitButton.setOnClickListener { cycleWeightUnit() }
        binding.weightCalcButton.setOnClickListener { calculateWeight() }
        binding.weightEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { calculateWeight() }
        })

        binding.tempUnitButton.setOnClickListener { swapTempUnit() }
        binding.tempCalcButton.setOnClickListener { calculateTemp() }
        binding.tempEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { calculateTemp() }
        })

        setupResultSizeSettings()
        applyResultTextSize()

        lifecycleScope.launch {
            val (rate, live) = fetchLiveRate()
            phpToUsdRate = rate
            rateIsLive = live
            updateRateLabel()
        }
    }

    private fun swapTravelCurrency() {
        travelPhp = !travelPhp
        applyTravelCurrencyLabels()
        binding.travelRateLabel.text = getString(R.string.fetching_rate)
        lifecycleScope.launch {
            val (rate, live) = fetchLiveRate()
            phpToUsdRate = rate
            rateIsLive = live
            updateRateLabel()
            calculateTravel()
        }
    }

    private fun applyTravelCurrencyLabels() {
        if (travelPhp) {
            binding.travelCurrencyHint.setText(R.string.travel_currency_php)
            binding.travelCostLabel.setText(R.string.trip_cost_php)
            binding.travelCostEntry.setHint(R.string.trip_cost_hint_php)
            binding.travelCurrencyButton.setText(R.string.travel_price_usd)
        } else {
            binding.travelCurrencyHint.setText(R.string.travel_currency_usd)
            binding.travelCostLabel.setText(R.string.trip_cost_usd)
            binding.travelCostEntry.setHint(R.string.trip_cost_hint_usd)
            binding.travelCurrencyButton.setText(R.string.travel_price_php)
        }
    }

    private fun setupResultSizeSettings() {
        when (ResultTextPrefs.get(this)) {
            ResultTextPrefs.Size.SMALL -> binding.sizeSmall.isChecked = true
            ResultTextPrefs.Size.MEDIUM -> binding.sizeMedium.isChecked = true
            ResultTextPrefs.Size.LARGE -> binding.sizeLarge.isChecked = true
            ResultTextPrefs.Size.EXTRA_LARGE -> binding.sizeXLarge.isChecked = true
        }
        binding.resultSizeGroup.setOnCheckedChangeListener { _, checkedId ->
            val size = when (checkedId) {
                R.id.sizeSmall -> ResultTextPrefs.Size.SMALL
                R.id.sizeMedium -> ResultTextPrefs.Size.MEDIUM
                R.id.sizeXLarge -> ResultTextPrefs.Size.EXTRA_LARGE
                else -> ResultTextPrefs.Size.LARGE
            }
            ResultTextPrefs.set(this, size)
            applyResultTextSize()
            val label = when (size) {
                ResultTextPrefs.Size.SMALL -> getString(R.string.size_small)
                ResultTextPrefs.Size.MEDIUM -> getString(R.string.size_medium)
                ResultTextPrefs.Size.LARGE -> getString(R.string.size_large)
                ResultTextPrefs.Size.EXTRA_LARGE -> getString(R.string.size_xlarge)
            }
            binding.settingsStatus.text = getString(R.string.settings_size_saved, label)
            binding.settingsStatus.setTextColor(Color.parseColor("#10B981"))
        }
    }

    private fun applyResultTextSize() {
        val size = ResultTextPrefs.get(this)
        ResultTextPrefs.apply(binding.resultLabel, null, size)
        ResultTextPrefs.apply(binding.travelResultLabel, binding.travelResultFxLabel, size)
        ResultTextPrefs.apply(binding.weightResultLabel, binding.weightResultSecondary, size)
        ResultTextPrefs.apply(binding.tempResultLabel, binding.tempResultSecondary, size)
    }

    private fun cToF(c: Double): Double = c * 9.0 / 5.0 + 32.0
    private fun fToC(f: Double): Double = (f - 32.0) * 5.0 / 9.0

    private fun swapTempUnit() {
        val raw = binding.tempEntry.text?.toString()?.trim().orEmpty()
        if (raw.isNotEmpty()) {
            raw.toDoubleOrNull()?.let { v ->
                val converted = if (tempCelsius) cToF(v) else fToC(v)
                val text = String.format(Locale.US, "%.2f", converted)
                    .trimEnd('0').trimEnd('.')
                binding.tempEntry.setText(text)
                binding.tempEntry.setSelection(binding.tempEntry.text?.length ?: 0)
            }
        }
        tempCelsius = !tempCelsius
        applyTempUnitLabels()
        calculateTemp()
    }

    private fun applyTempUnitLabels() {
        if (tempCelsius) {
            binding.tempUnitLabel.setText(R.string.temp_label_c)
            binding.tempUnitButton.setText(R.string.temp_switch_to_f)
            binding.tempEntry.setHint(R.string.temp_hint_entry_c)
            binding.tempCalcButton.setText(R.string.temp_convert_to_f)
            binding.tempHint.setText(R.string.temp_hint_c)
        } else {
            binding.tempUnitLabel.setText(R.string.temp_label_f)
            binding.tempUnitButton.setText(R.string.temp_switch_to_c)
            binding.tempEntry.setHint(R.string.temp_hint_entry_f)
            binding.tempCalcButton.setText(R.string.temp_convert_to_c)
            binding.tempHint.setText(R.string.temp_hint_f)
        }
    }

    private fun calculateTemp() {
        val raw = binding.tempEntry.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) {
            binding.tempResultLabel.text = "—"
            binding.tempResultLabel.setTextColor(Color.parseColor("#3B82F6"))
            binding.tempResultSecondary.text = ""
            binding.tempStatus.setText(R.string.temp_enter)
            binding.tempStatus.setTextColor(Color.parseColor("#9CA3AF"))
            return
        }
        val value = raw.toDoubleOrNull()
        if (value == null) {
            binding.tempResultLabel.text = getString(R.string.invalid_input)
            binding.tempResultLabel.setTextColor(Color.parseColor("#EF4444"))
            binding.tempResultSecondary.text = ""
            binding.tempStatus.setText(R.string.temp_invalid)
            binding.tempStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        val fromLabel = String.format(Locale.US, "%g", value)
        if (tempCelsius) {
            val f = cToF(value)
            binding.tempResultLabel.text = getString(R.string.temp_result_f, f)
            binding.tempResultSecondary.text = getString(R.string.temp_from_c, fromLabel)
        } else {
            val c = fToC(value)
            binding.tempResultLabel.text = getString(R.string.temp_result_c, c)
            binding.tempResultSecondary.text = getString(R.string.temp_from_f, fromLabel)
        }
        binding.tempResultLabel.setTextColor(Color.parseColor("#10B981"))
        binding.tempStatus.setText(R.string.temp_note)
        binding.tempStatus.setTextColor(Color.parseColor("#9CA3AF"))
    }

    private fun cycleWeightUnit() {
        val order = listOf("lb", "kg", "g")
        val raw = binding.weightEntry.text?.toString()?.trim().orEmpty()
        val old = weightUnit
        val new = order[(order.indexOf(old) + 1) % order.size]
        if (raw.isNotEmpty()) {
            raw.toDoubleOrNull()?.takeIf { it >= 0 }?.let { v ->
                val kg = toKg(v, old)
                val converted = fromKg(kg)[new] ?: 0.0
                val text = if (new == "g" && converted >= 1) {
                    String.format(Locale.US, "%.2f", converted)
                } else {
                    String.format(Locale.US, "%.4f", converted).trimEnd('0').trimEnd('.')
                }
                binding.weightEntry.setText(text)
                binding.weightEntry.setSelection(binding.weightEntry.text?.length ?: 0)
            }
        }
        weightUnit = new
        applyWeightUnitLabels()
        calculateWeight()
    }

    private fun applyWeightUnitLabels() {
        when (weightUnit) {
            "kg" -> {
                binding.weightUnitLabel.setText(R.string.weight_kg)
                binding.weightUnitButton.setText(R.string.weight_switch_to_g)
                binding.weightEntry.setHint(R.string.weight_hint_kg)
            }
            "g" -> {
                binding.weightUnitLabel.setText(R.string.weight_g)
                binding.weightUnitButton.setText(R.string.weight_switch_to_lb)
                binding.weightEntry.setHint(R.string.weight_hint_g)
            }
            else -> {
                binding.weightUnitLabel.setText(R.string.weight_lb)
                binding.weightUnitButton.setText(R.string.weight_switch_to_kg)
                binding.weightEntry.setHint(R.string.weight_hint_lb)
            }
        }
        binding.weightHint.text =
            getString(R.string.weight_hint) + "  (input is $weightUnit)"
    }

    private fun toKg(value: Double, unit: String): Double = when (unit) {
        "lb" -> value * KG_PER_LB
        "kg" -> value
        "g" -> value / G_PER_KG
        else -> value
    }

    private fun fromKg(kg: Double): Map<String, Double> = mapOf(
        "kg" to kg,
        "g" to kg * G_PER_KG,
        "lb" to kg / KG_PER_LB
    )

    private fun formatWeight(unit: String, v: Double): String = when (unit) {
        "g" -> if (v >= 0.01) String.format(Locale.US, "%,.2f g", v)
        else String.format(Locale.US, "%.6f g", v)
        "kg" -> String.format(Locale.US, "%,.4f kg", v)
        else -> String.format(Locale.US, "%,.4f lb", v)
    }

    private fun calculateWeight() {
        val raw = binding.weightEntry.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) {
            binding.weightResultLabel.text = "—"
            binding.weightResultLabel.setTextColor(Color.parseColor("#3B82F6"))
            binding.weightResultSecondary.text = ""
            binding.weightStatus.setText(R.string.weight_enter)
            binding.weightStatus.setTextColor(Color.parseColor("#9CA3AF"))
            return
        }
        val value = raw.toDoubleOrNull()
        if (value == null || value < 0) {
            binding.weightResultLabel.text = getString(R.string.invalid_input)
            binding.weightResultLabel.setTextColor(Color.parseColor("#EF4444"))
            binding.weightResultSecondary.text = ""
            binding.weightStatus.setText(R.string.weight_invalid)
            binding.weightStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        val all = fromKg(toKg(value, weightUnit))
        val others = listOf("lb", "kg", "g").filter { it != weightUnit }
        binding.weightResultLabel.text = formatWeight(others[0], all[others[0]]!!)
        binding.weightResultLabel.setTextColor(Color.parseColor("#10B981"))
        binding.weightResultSecondary.text =
            formatWeight(others[1], all[others[1]]!!) +
                "\n(from ${formatWeight(weightUnit, all[weightUnit]!!)})"
        binding.weightStatus.setText(R.string.weight_note)
        binding.weightStatus.setTextColor(Color.parseColor("#9CA3AF"))
    }

    private fun swapDirection() {
        phpToUsd = !phpToUsd
        applyDirectionLabels()
        binding.rateInfoLabel.text = getString(R.string.fetching_rate)
        binding.rateInfoLabel.setTextColor(Color.parseColor("#9CA3AF"))
        lifecycleScope.launch {
            val (rate, live) = fetchLiveRate()
            phpToUsdRate = rate
            rateIsLive = live
            updateRateLabel()
            val raw = binding.amountEntry.text?.toString()?.trim().orEmpty()
            if (raw.isNotEmpty()) {
                convertCurrency()
            } else {
                binding.resultLabel.text = if (phpToUsd) {
                    getString(R.string.default_result_usd)
                } else {
                    getString(R.string.default_result_php)
                }
                binding.resultLabel.setTextColor(Color.parseColor("#3B82F6"))
            }
            calculateTravel()
        }
    }

    private fun applyDirectionLabels() {
        if (phpToUsd) {
            binding.subtitleLabel.setText(R.string.subtitle_php_to_usd)
            binding.inputLabel.setText(R.string.amount_label_php)
            binding.amountEntry.setHint(R.string.amount_hint_php)
            binding.convertButton.setText(R.string.convert_to_usd)
            binding.swapButton.setText(R.string.swap_to_usd)
        } else {
            binding.subtitleLabel.setText(R.string.subtitle_usd_to_php)
            binding.inputLabel.setText(R.string.amount_label_usd)
            binding.amountEntry.setHint(R.string.amount_hint_usd)
            binding.convertButton.setText(R.string.convert_to_php)
            binding.swapButton.setText(R.string.swap_to_php)
        }
    }

    private fun updateRateLabel() {
        val base = if (phpToUsd) {
            getString(R.string.rate_php_to_usd, phpToUsdRate)
        } else {
            val usdToPhp = if (phpToUsdRate > 0) 1.0 / phpToUsdRate else 0.0
            getString(R.string.rate_usd_to_php, usdToPhp)
        }
        val text = if (rateIsLive) base else "$base · ${getString(R.string.using_fallback)}"
        binding.rateInfoLabel.setTextColor(Color.parseColor("#9CA3AF"))
        binding.rateInfoLabel.text = text
        binding.travelRateLabel.setTextColor(Color.parseColor("#9CA3AF"))
        binding.travelRateLabel.text = text
    }

    private fun convertCurrency() {
        val raw = binding.amountEntry.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) {
            binding.resultLabel.text = if (phpToUsd) {
                getString(R.string.default_result_usd)
            } else {
                getString(R.string.default_result_php)
            }
            binding.resultLabel.setTextColor(Color.parseColor("#3B82F6"))
            binding.rateInfoLabel.text = getString(R.string.enter_amount)
            binding.rateInfoLabel.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        val amount = raw.toDoubleOrNull()
        if (amount == null || amount < 0.0) {
            binding.resultLabel.text = getString(R.string.invalid_input)
            binding.resultLabel.setTextColor(Color.parseColor("#EF4444"))
            binding.rateInfoLabel.text = getString(R.string.invalid_hint)
            binding.rateInfoLabel.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        if (phpToUsd) {
            binding.resultLabel.text =
                String.format(Locale.US, "$%,.2f USD", amount * phpToUsdRate)
        } else {
            val php = if (phpToUsdRate > 0) amount / phpToUsdRate else 0.0
            binding.resultLabel.text = String.format(Locale.US, "₱%,.2f PHP", php)
        }
        binding.resultLabel.setTextColor(Color.parseColor("#10B981"))
        updateRateLabel()
    }

    // --- Travel ---

    private fun swapDistanceUnit() {
        val raw = binding.distanceEntry.text?.toString()?.trim().orEmpty()
        val oldUseKm = useKm
        useKm = !useKm
        if (raw.isNotEmpty()) {
            raw.toDoubleOrNull()?.takeIf { it >= 0 }?.let { v ->
                val converted = if (oldUseKm && !useKm) {
                    v / KM_PER_MILE
                } else if (!oldUseKm && useKm) {
                    v * KM_PER_MILE
                } else v
                binding.distanceEntry.setText(String.format(Locale.US, "%.2f", converted))
                binding.distanceEntry.setSelection(binding.distanceEntry.text?.length ?: 0)
            }
        }
        applyDistanceUnitLabels()
        calculateTravel()
    }

    private fun applyDistanceUnitLabels() {
        if (useKm) {
            binding.distanceUnitLabel.setText(R.string.distance_km)
            binding.unitSwitchButton.setText(R.string.switch_to_miles)
            binding.travelCalcButton.setText(R.string.calc_per_km)
        } else {
            binding.distanceUnitLabel.setText(R.string.distance_mi)
            binding.unitSwitchButton.setText(R.string.switch_to_km)
            binding.travelCalcButton.setText(R.string.calc_per_mi)
        }
    }

    private fun updateDistanceEquiv(distance: Double?) {
        if (distance == null || distance < 0) {
            binding.distanceEquivLabel.text = ""
            return
        }
        binding.distanceEquivLabel.text = if (useKm) {
            getString(R.string.equiv_mi, distance / KM_PER_MILE)
        } else {
            getString(R.string.equiv_km, distance * KM_PER_MILE)
        }
        // subtle parentheses style in string already as ≈
        binding.distanceEquivLabel.text = "(${binding.distanceEquivLabel.text})"
    }

    private fun calculateTravel() {
        val distRaw = binding.distanceEntry.text?.toString()?.trim().orEmpty()
        val costRaw = binding.travelCostEntry.text?.toString()?.trim().orEmpty()
        val dist = distRaw.toDoubleOrNull()
        updateDistanceEquiv(dist)

        if (distRaw.isEmpty() || costRaw.isEmpty()) {
            binding.travelResultLabel.text = if (useKm) {
                getString(R.string.travel_result_placeholder_km)
            } else {
                getString(R.string.travel_result_placeholder_mi)
            }
            binding.travelResultLabel.setTextColor(Color.parseColor("#3B82F6"))
            binding.travelResultFxLabel.text = ""
            binding.travelCostFxLabel.text = ""
            binding.travelStatusLabel.setText(R.string.travel_enter_both)
            binding.travelStatusLabel.setTextColor(Color.parseColor("#9CA3AF"))
            return
        }

        val cost = costRaw.toDoubleOrNull()
        if (dist == null || cost == null || dist <= 0.0 || cost < 0.0) {
            binding.travelResultLabel.text = getString(R.string.invalid_input)
            binding.travelResultLabel.setTextColor(Color.parseColor("#EF4444"))
            binding.travelResultFxLabel.text = ""
            binding.travelCostFxLabel.text = ""
            binding.travelStatusLabel.setText(R.string.travel_invalid)
            binding.travelStatusLabel.setTextColor(Color.parseColor("#EF4444"))
            return
        }

        val unit = if (useKm) "km" else "mi"
        val per = cost / dist

        if (travelPhp) {
            val costUsd = cost * phpToUsdRate
            val perUsd = per * phpToUsdRate
            binding.travelCostFxLabel.text =
                String.format(Locale.US, "(≈ $%,.2f USD)", costUsd)
            binding.travelResultLabel.text = if (useKm) {
                getString(R.string.travel_per_php_km, per)
            } else {
                getString(R.string.travel_per_php_mi, per)
            }
            binding.travelResultFxLabel.text =
                String.format(Locale.US, "(≈ $%.2f USD / %s)", perUsd, unit)
        } else {
            val costPhp = if (phpToUsdRate > 0) cost / phpToUsdRate else 0.0
            val perPhp = if (phpToUsdRate > 0) per / phpToUsdRate else 0.0
            binding.travelCostFxLabel.text =
                String.format(Locale.US, "(≈ ₱%,.2f PHP)", costPhp)
            binding.travelResultLabel.text = if (useKm) {
                getString(R.string.travel_per_usd_km, per)
            } else {
                getString(R.string.travel_per_usd_mi, per)
            }
            binding.travelResultFxLabel.text =
                String.format(Locale.US, "(≈ ₱%.2f PHP / %s)", perPhp, unit)
        }
        binding.travelResultLabel.setTextColor(Color.parseColor("#10B981"))
        binding.travelStatusLabel.setText(R.string.travel_status_ok)
        binding.travelStatusLabel.setTextColor(Color.parseColor("#9CA3AF"))
        updateRateLabel()
    }

    private suspend fun fetchLiveRate(): Pair<Double, Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL(RATE_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }
            conn.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                val json = JSONObject(body)
                json.getJSONObject("rates").getDouble("USD") to true
            }
        } catch (_: Exception) {
            FALLBACK_RATE to false
        }
    }

    companion object {
        private const val RATE_URL = "https://api.exchangerate-api.com/v4/latest/PHP"
        private const val FALLBACK_RATE = 0.0175
        private const val KM_PER_MILE = 1.609344
        private const val KG_PER_LB = 0.45359237
        private const val G_PER_KG = 1000.0
    }
}
