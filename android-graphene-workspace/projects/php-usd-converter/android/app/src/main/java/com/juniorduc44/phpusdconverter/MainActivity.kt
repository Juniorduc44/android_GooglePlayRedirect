package com.juniorduc44.phpusdconverter

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
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
    private var chainCategoryId: String = RobinhoodChainTracker.DEFAULT_CATEGORY
    private var chainSpinnerReady: Boolean = false
    private var chainLoadedOnce: Boolean = false
    private var specTokenRows: List<RobinhoodChainTracker.AssetQuote> = emptyList()
    private var specLiveSupply: Double? = null
    private var specLiveMcap: Double? = null
    private var specLivePrice: Double? = null
    private var specLiveSymbol: String = ""
    private var specTokenSpinnerReady: Boolean = false
    private var specSuppressWatcher: Boolean = false
    /** ViewFlipper index of the active tool section */
    private var currentSection: Int = SECTION_CONVERT

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
        setupChainTab()
        setupSpecTab()

        // Sandwich menu (top-right) — all tools + Settings live here (no top tab strip)
        binding.menuButton.setOnClickListener { showNavMenu() }
        showSection(SECTION_CONVERT, fromMenu = false)

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

        binding.chainRefreshButton.setOnClickListener { refreshBlockchain() }
        binding.chainSelfTestButton.setOnClickListener { runBlockchainSelfTest() }
        binding.chainAddContractButton.setOnClickListener { addChainContract() }
        binding.chainDefaultButton.setOnClickListener { saveChainDefaultCategory() }

        setupResultSizeSettings()
        applyResultTextSize()

        lifecycleScope.launch {
            val (rate, live) = fetchLiveRate()
            phpToUsdRate = rate
            rateIsLive = live
            updateRateLabel()
        }
    }

    // --- Navigation (hamburger / sandwich menu) ---

    private fun showNavMenu() {
        val popup = PopupMenu(this, binding.menuButton, Gravity.END)
        popup.menu.add(0, SECTION_CONVERT, 0, R.string.section_convert)
        popup.menu.add(0, SECTION_TRAVEL, 1, R.string.section_travel)
        popup.menu.add(0, SECTION_WEIGHT, 2, R.string.section_weight)
        popup.menu.add(0, SECTION_TEMP, 3, R.string.section_temp)
        popup.menu.add(0, SECTION_SPEC, 4, R.string.section_spec)
        popup.menu.add(0, SECTION_CHAIN, 5, R.string.section_chain)
        popup.menu.add(0, SECTION_SETTINGS, 6, R.string.section_settings)
        // Mark current
        popup.menu.findItem(currentSection)?.isChecked = true
        popup.menu.setGroupCheckable(0, true, true)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            showSection(item.itemId, fromMenu = true)
            true
        }
        popup.show()
    }

    private fun showSection(index: Int, fromMenu: Boolean) {
        if (index !in 0..6) return
        currentSection = index
        binding.tabFlipper.displayedChild = index
        when (index) {
            SECTION_CONVERT -> {
                binding.titleLabel.setText(R.string.section_convert)
                // subtitle set by applyDirectionLabels
                applyDirectionLabels()
            }
            SECTION_TRAVEL -> {
                binding.titleLabel.setText(R.string.section_travel)
                binding.subtitleLabel.text = getString(R.string.travel_currency_php)
                calculateTravel()
            }
            SECTION_WEIGHT -> {
                binding.titleLabel.setText(R.string.section_weight)
                binding.subtitleLabel.setText(R.string.weight_hint)
                calculateWeight()
            }
            SECTION_TEMP -> {
                binding.titleLabel.setText(R.string.section_temp)
                binding.subtitleLabel.setText(R.string.temp_hint_c)
                calculateTemp()
            }
            SECTION_SPEC -> {
                binding.titleLabel.setText(R.string.section_spec)
                binding.subtitleLabel.setText(R.string.spec_subtitle)
                calculateSpecAll()
            }
            SECTION_CHAIN -> {
                binding.titleLabel.setText(R.string.section_chain)
                binding.subtitleLabel.text = "Robinhood · ${RobinhoodChainTracker.CHAIN_ID}"
                // Only auto-fetch markets once (DexScreener can be slow)
                if (!chainLoadedOnce) refreshBlockchain()
            }
            SECTION_SETTINGS -> {
                binding.titleLabel.setText(R.string.section_settings)
                binding.subtitleLabel.text = getString(R.string.settings_display)
            }
        }
    }


    // --- Spec (item / token perspectives) ---

    private fun setupSpecTab() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!specSuppressWatcher) calculateSpecAll()
            }
        }
        binding.specMcap.addTextChangedListener(watcher)
        binding.specSupply.addTextChangedListener(watcher)
        binding.specPrice.addTextChangedListener(watcher)
        binding.specSpent.addTextChangedListener(watcher)
        binding.specHoldings.addTextChangedListener(watcher)
        binding.specTarget.addTextChangedListener(watcher)
        binding.specCostBasis.addTextChangedListener(watcher)
        binding.specCopyPriceButton.setOnClickListener { specCopyPriceAtoB() }
        binding.specCopyItemsButton.setOnClickListener { specCopyItemsBtoC() }

        val srcLabels = RobinhoodChainTracker.categoryLabels()
            .filter { it != "My contracts" }
            .toTypedArray()
        binding.specSourceSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, srcLabels)
        // default trending boosts if present
        val boostIdx = srcLabels.indexOfFirst { it.contains("boost", ignoreCase = true) }
        if (boostIdx >= 0) binding.specSourceSpinner.setSelection(boostIdx)

        binding.specTokenSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.spec_mkt_live_idle).replace("Live: ", "")),
        )
        binding.specTokenSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long,
                ) {
                    if (!specTokenSpinnerReady) return
                    onSpecTokenSelected(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.specLoadButton.setOnClickListener { loadSpecMarket() }
        binding.specApplyLockedButton.setOnClickListener { applySpecLocked(onlyLocked = false) }
        binding.specClearLocksButton.setOnClickListener { clearSpecLocks() }
        binding.specLockSupply.setOnCheckedChangeListener { _, _ ->
            applySpecEntryStates()
            applySpecLocked(onlyLocked = true)
        }
        binding.specLockMcap.setOnCheckedChangeListener { _, _ ->
            applySpecEntryStates()
            applySpecLocked(onlyLocked = true)
        }
        binding.specLockPrice.setOnCheckedChangeListener { _, _ ->
            applySpecEntryStates()
            applySpecLocked(onlyLocked = true)
        }
        applySpecEntryStates()
    }

    private fun loadSpecMarket() {
        val label = binding.specSourceSpinner.selectedItem?.toString()
            ?: return
        val cat = RobinhoodChainTracker.categoryIdForLabel(label)
        binding.specLoadButton.isEnabled = false
        binding.specMktStatus.text = "Loading DexScreener…"
        binding.specMktStatus.setTextColor(Color.parseColor("#9CA3AF"))
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) {
                try {
                    RobinhoodChainTracker.fetchCategory(cat, limit = 15)
                        .filter { it.error == null && (it.priceUsd != null || it.address.isNotEmpty()) }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            binding.specLoadButton.isEnabled = true
            if (rows.isEmpty()) {
                binding.specMktStatus.text = "No priced tokens — try another source."
                binding.specMktStatus.setTextColor(Color.parseColor("#F59E0B"))
                return@launch
            }
            specTokenRows = rows
            val labels = rows.map { specTokenLabel(it) }
            specTokenSpinnerReady = false
            binding.specTokenSpinner.adapter =
                ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            binding.specTokenSpinner.setSelection(0)
            specTokenSpinnerReady = true
            onSpecTokenSelected(0)
            binding.specMktStatus.text = "Loaded ${rows.size} · pick token · toggle locks · Apply"
            binding.specMktStatus.setTextColor(Color.parseColor("#10B981"))
        }
    }

    private fun specTokenLabel(a: RobinhoodChainTracker.AssetQuote): String {
        val px = a.priceUsd?.let { SpeculatorMath.formatMoney(it) } ?: "n/a"
        val sup = a.estimatedSupply
            ?: RobinhoodChainTracker.estimateSupply(a.priceUsd, a.marketCap, a.fdv)
        val supS = sup?.let { SpeculatorMath.formatQty(it) } ?: "?"
        return "${a.symbol}  ·  $px  ·  sup~$supS"
    }

    private fun onSpecTokenSelected(position: Int) {
        val asset = specTokenRows.getOrNull(position) ?: return
        val price = asset.priceUsd
        val mcap = asset.marketCap ?: asset.fdv
        val supply = asset.estimatedSupply
            ?: RobinhoodChainTracker.estimateSupply(price, asset.marketCap, asset.fdv)
        specLiveSymbol = asset.symbol
        specLivePrice = price
        specLiveMcap = mcap
        specLiveSupply = supply
        val bits = mutableListOf(asset.symbol)
        if (price != null) bits.add("price ${SpeculatorMath.formatMoney(price)}")
        if (mcap != null) bits.add("mcap ${SpeculatorMath.formatMoney(mcap)}")
        if (supply != null) bits.add("supply ~${SpeculatorMath.formatQty(supply)}")
        if (asset.address.isNotEmpty()) bits.add(asset.address.take(10) + "…")
        binding.specLiveLabel.text = "Live: " + bits.joinToString(" · ")
        applySpecLocked(onlyLocked = false)
    }

    private fun applySpecEntryStates() {
        binding.specSupply.isEnabled = !binding.specLockSupply.isChecked
        binding.specMcap.isEnabled = !binding.specLockMcap.isChecked
        binding.specPrice.isEnabled = !binding.specLockPrice.isChecked
    }

    private fun setSpecEntry(edit: android.widget.EditText, value: Double?) {
        specSuppressWatcher = true
        if (value == null) {
            // leave as-is
        } else {
            edit.setText(SpeculatorMath.formatRaw(value))
        }
        specSuppressWatcher = false
    }

    private fun applySpecLocked(onlyLocked: Boolean) {
        if (specLiveSymbol.isEmpty() &&
            specLiveSupply == null && specLiveMcap == null && specLivePrice == null
        ) {
            if (!onlyLocked) {
                binding.specMktStatus.text = "Select a loaded token first."
                binding.specMktStatus.setTextColor(Color.parseColor("#F59E0B"))
            }
            applySpecEntryStates()
            return
        }
        // write while enabled
        binding.specSupply.isEnabled = true
        binding.specMcap.isEnabled = true
        binding.specPrice.isEnabled = true

        if (binding.specLockSupply.isChecked && specLiveSupply != null) {
            setSpecEntry(binding.specSupply, specLiveSupply)
        } else if (!onlyLocked && binding.specSupply.text.isNullOrBlank() && specLiveSupply != null) {
            setSpecEntry(binding.specSupply, specLiveSupply)
        }

        if (binding.specLockMcap.isChecked && specLiveMcap != null) {
            setSpecEntry(binding.specMcap, specLiveMcap)
        } else if (!onlyLocked && !binding.specLockMcap.isChecked &&
            binding.specMcap.text.isNullOrBlank() && specLiveMcap != null
        ) {
            setSpecEntry(binding.specMcap, specLiveMcap)
        }

        if (binding.specLockPrice.isChecked && specLivePrice != null) {
            setSpecEntry(binding.specPrice, specLivePrice)
        } else if (!onlyLocked && !binding.specLockPrice.isChecked &&
            binding.specPrice.text.isNullOrBlank() && specLivePrice != null
        ) {
            setSpecEntry(binding.specPrice, specLivePrice)
        }

        applySpecEntryStates()
        val locked = buildList {
            if (binding.specLockSupply.isChecked) add("supply")
            if (binding.specLockMcap.isChecked) add("mcap")
            if (binding.specLockPrice.isChecked) add("price")
        }
        val free = buildList {
            if (!binding.specLockSupply.isChecked) add("supply")
            if (!binding.specLockMcap.isChecked) add("mcap")
            if (!binding.specLockPrice.isChecked) add("price")
        }
        val sym = specLiveSymbol.ifEmpty { "token" }
        binding.specMktStatus.text =
            "$sym: locked [${locked.joinToString(", ").ifEmpty { "none" }}] · edit [${free.joinToString(", ").ifEmpty { "none" }}]"
        binding.specMktStatus.setTextColor(Color.parseColor("#10B981"))
        calculateSpecAll()
    }

    private fun clearSpecLocks() {
        binding.specLockSupply.isChecked = false
        binding.specLockMcap.isChecked = false
        binding.specLockPrice.isChecked = false
        applySpecEntryStates()
        binding.specMktStatus.text = "All fields unlocked — type freely."
        binding.specMktStatus.setTextColor(Color.parseColor("#9CA3AF"))
    }

    private fun specCopyPriceAtoB() {
        val mcap = SpeculatorMath.parseNumber(binding.specMcap.text?.toString())
        val supply = SpeculatorMath.parseNumber(binding.specSupply.text?.toString())
        if (mcap == null || supply == null || supply == 0.0) {
            binding.specAStatus.text = "Need valid market cap and non-zero supply first."
            binding.specAStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        val px = SpeculatorMath.priceFromMcap(mcap, supply)
        binding.specPrice.setText(SpeculatorMath.formatRaw(px))
        calculateSpecAll()
    }

    private fun specCopyItemsBtoC() {
        val price = SpeculatorMath.parseNumber(binding.specPrice.text?.toString())
        val spent = SpeculatorMath.parseNumber(binding.specSpent.text?.toString())
        if (price == null || spent == null || price == 0.0) {
            binding.specBStatus.text = "Need valid price and spend first."
            binding.specBStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        val items = SpeculatorMath.itemsFromSpend(spent, price)
        binding.specHoldings.setText(SpeculatorMath.formatRaw(items))
        if (binding.specCostBasis.text.isNullOrBlank()) {
            binding.specCostBasis.setText(SpeculatorMath.formatRaw(spent))
        }
        calculateSpecAll()
    }

    private fun calculateSpecAll() {
        calculateSpecA()
        calculateSpecB()
        calculateSpecC()
    }

    private fun calculateSpecA() {
        val mcap = SpeculatorMath.parseNumber(binding.specMcap.text?.toString())
        val supply = SpeculatorMath.parseNumber(binding.specSupply.text?.toString())
        if (mcap == null && supply == null) {
            binding.specAResult.text = "—"
            binding.specASecondary.text = ""
            binding.specAStatus.setText(R.string.spec_a_status)
            binding.specAStatus.setTextColor(Color.parseColor("#9CA3AF"))
            return
        }
        if (mcap == null || supply == null) {
            binding.specAResult.text = "—"
            binding.specASecondary.text = ""
            binding.specAStatus.text = "Fill both market cap and supply."
            binding.specAStatus.setTextColor(Color.parseColor("#F59E0B"))
            return
        }
        if (supply == 0.0) {
            binding.specAResult.text = "—"
            binding.specASecondary.text = ""
            binding.specAStatus.text = "Supply cannot be zero."
            binding.specAStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        if (mcap < 0 || supply < 0) {
            binding.specAStatus.text = "Use non-negative numbers."
            binding.specAStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        val px = SpeculatorMath.priceFromMcap(mcap, supply)
        val rev = SpeculatorMath.mcapFromPrice(px, supply)
        binding.specAResult.text = SpeculatorMath.formatMoney(px) + " / item"
        binding.specASecondary.text =
            "Check: ${SpeculatorMath.formatMoney(px)} × ${SpeculatorMath.formatQty(supply)} ≈ ${SpeculatorMath.formatMoney(rev)} mcap"
        binding.specAStatus.text =
            "At mcap ${SpeculatorMath.formatMoney(mcap)} with supply ${SpeculatorMath.formatQty(supply)}"
        binding.specAStatus.setTextColor(Color.parseColor("#9CA3AF"))
    }

    private fun calculateSpecB() {
        val price = SpeculatorMath.parseNumber(binding.specPrice.text?.toString())
        val spent = SpeculatorMath.parseNumber(binding.specSpent.text?.toString())
        if (price == null && spent == null) {
            binding.specBResult.text = "—"
            binding.specBSecondary.text = ""
            binding.specBStatus.setText(R.string.spec_b_status)
            binding.specBStatus.setTextColor(Color.parseColor("#9CA3AF"))
            return
        }
        if (price == null || spent == null) {
            binding.specBResult.text = "—"
            binding.specBSecondary.text = ""
            binding.specBStatus.text = "Fill both price and amount spent."
            binding.specBStatus.setTextColor(Color.parseColor("#F59E0B"))
            return
        }
        if (price == 0.0) {
            binding.specBResult.text = "—"
            binding.specBSecondary.text = ""
            binding.specBStatus.text = "Price cannot be zero."
            binding.specBStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        if (price < 0 || spent < 0) {
            binding.specBStatus.text = "Use non-negative numbers."
            binding.specBStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        val items = SpeculatorMath.itemsFromSpend(spent, price)
        val cost = SpeculatorMath.costForItems(price, items)
        binding.specBResult.text = SpeculatorMath.formatQty(items) + " items"
        binding.specBSecondary.text =
            "Check: ${SpeculatorMath.formatQty(items)} × ${SpeculatorMath.formatMoney(price)} ≈ ${SpeculatorMath.formatMoney(cost)}"
        binding.specBStatus.text =
            "Spending ${SpeculatorMath.formatMoney(spent)} at ${SpeculatorMath.formatMoney(price)} each"
        binding.specBStatus.setTextColor(Color.parseColor("#9CA3AF"))
    }

    private fun calculateSpecC() {
        val holdings = SpeculatorMath.parseNumber(binding.specHoldings.text?.toString())
        val target = SpeculatorMath.parseNumber(binding.specTarget.text?.toString())
        val basis = SpeculatorMath.parseNumber(binding.specCostBasis.text?.toString())
        if (holdings == null && target == null) {
            binding.specCResult.text = "—"
            binding.specCSecondary.text = ""
            binding.specCStatus.setText(R.string.spec_c_status)
            binding.specCStatus.setTextColor(Color.parseColor("#9CA3AF"))
            return
        }
        if (holdings == null || target == null) {
            binding.specCResult.text = "—"
            binding.specCSecondary.text = ""
            binding.specCStatus.text = "Fill holdings and target price."
            binding.specCStatus.setTextColor(Color.parseColor("#F59E0B"))
            return
        }
        if (holdings < 0 || target < 0) {
            binding.specCStatus.text = "Use non-negative numbers."
            binding.specCStatus.setTextColor(Color.parseColor("#EF4444"))
            return
        }
        val total = SpeculatorMath.valueAtTarget(holdings, target)
        binding.specCResult.text = SpeculatorMath.formatMoney(total) + " total"
        val bits = mutableListOf(
            "${SpeculatorMath.formatQty(holdings)} items × ${SpeculatorMath.formatMoney(target)}",
        )
        if (basis != null && basis >= 0 && holdings != 0.0) {
            val ac = SpeculatorMath.avgCost(basis, holdings)
            val pnl = SpeculatorMath.pnlAtTarget(holdings, target, basis)
            val sign = if (pnl >= 0) "+" else ""
            bits.add(
                "avg cost ${SpeculatorMath.formatMoney(ac)} · P/L $sign${SpeculatorMath.formatMoney(pnl)}",
            )
        }
        binding.specCSecondary.text = bits.joinToString(" · ")
        binding.specCStatus.text = "Speculative value at your target price (not advice)."
        binding.specCStatus.setTextColor(Color.parseColor("#9CA3AF"))
    }

    // --- Blockchain (Robinhood Chain) ---

    private fun setupChainTab() {
        binding.chainMetaLabel.text = RobinhoodChainTracker.networkMetaLine()
        val prefs = getSharedPreferences("chain_prefs", MODE_PRIVATE)
        chainCategoryId = prefs.getString("default_category", RobinhoodChainTracker.DEFAULT_CATEGORY)
            ?: RobinhoodChainTracker.DEFAULT_CATEGORY

        val labels = RobinhoodChainTracker.categoryLabels()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        binding.chainCategorySpinner.adapter = adapter
        val idx = labels.indexOf(RobinhoodChainTracker.categoryLabelForId(chainCategoryId)).coerceAtLeast(0)
        binding.chainCategorySpinner.setSelection(idx)
        binding.chainCategoryDesc.text = RobinhoodChainTracker.categoryDescription(chainCategoryId)

        binding.chainCategorySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long,
                ) {
                    if (!chainSpinnerReady) {
                        chainSpinnerReady = true
                        return
                    }
                    val label = labels.getOrNull(position) ?: return
                    chainCategoryId = RobinhoodChainTracker.categoryIdForLabel(label)
                    binding.chainCategoryDesc.text =
                        RobinhoodChainTracker.categoryDescription(chainCategoryId)
                    refreshBlockchain()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // empty state card
        renderCoinCards(emptyList(), placeholder = getString(R.string.chain_status_idle))
    }

    private fun saveChainDefaultCategory() {
        getSharedPreferences("chain_prefs", MODE_PRIVATE)
            .edit()
            .putString("default_category", chainCategoryId)
            .apply()
        binding.chainStatus.text = getString(
            R.string.chain_default_saved,
            RobinhoodChainTracker.categoryLabelForId(chainCategoryId),
        )
        binding.chainStatus.setTextColor(Color.parseColor("#10B981"))
    }

    private fun setChainBusy(busy: Boolean) {
        binding.chainRefreshButton.isEnabled = !busy
        binding.chainSelfTestButton.isEnabled = !busy
        binding.chainAddContractButton.isEnabled = !busy
        binding.chainDefaultButton.isEnabled = !busy
        binding.chainCategorySpinner.isEnabled = !busy
        binding.chainLivePill.text =
            if (busy) getString(R.string.chain_loading_pill) else getString(R.string.chain_live)
    }

    private fun refreshBlockchain() {
        setChainBusy(true)
        binding.chainStatus.setText(R.string.chain_status_loading)
        binding.chainStatus.setTextColor(Color.parseColor("#9CA3AF"))
        val cat = chainCategoryId
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Result.success(RobinhoodChainTracker.fetchCategory(cat, 10))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            setChainBusy(false)
            result.fold(
                onSuccess = { rows ->
                    try {
                        chainLoadedOnce = true
                        renderCoinCards(rows)
                        val ok = rows.count { it.error == null && (it.priceUsd != null || it.address.isNotEmpty()) }
                        binding.chainStatus.text = getString(
                            R.string.chain_status_loaded,
                            RobinhoodChainTracker.categoryLabelForId(cat),
                            ok,
                            rows.size,
                        )
                        binding.chainStatus.setTextColor(
                            Color.parseColor(if (ok > 0) "#10B981" else "#F59E0B")
                        )
                    } catch (e: Exception) {
                        binding.chainStatus.text = "UI update failed: ${e.message}"
                        binding.chainStatus.setTextColor(Color.parseColor("#EF4444"))
                    }
                },
                onFailure = { e ->
                    binding.chainStatus.text =
                        "Refresh failed: ${e.javaClass.simpleName}: ${e.message}"
                    binding.chainStatus.setTextColor(Color.parseColor("#EF4444"))
                    renderCoinCards(emptyList(), placeholder = e.message ?: "Error")
                },
            )
        }
    }

    private fun renderCoinCards(
        rows: List<RobinhoodChainTracker.AssetQuote>,
        placeholder: String? = null,
    ) {
        val container = binding.chainCoinsContainer
        container.removeAllViews()
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        if (rows.isEmpty()) {
            val empty = MaterialCardView(this).apply {
                radius = dp(14).toFloat()
                setCardBackgroundColor(Color.parseColor("#1E293B"))
                cardElevation = 0f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also { it.bottomMargin = dp(8) }
            }
            val tv = TextView(this).apply {
                text = placeholder ?: getString(R.string.chain_not_loaded)
                setTextColor(Color.parseColor("#64748B"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(28), dp(16), dp(28))
            }
            empty.addView(tv)
            container.addView(empty)
            return
        }

        rows.forEachIndexed { index, a ->
            val card = MaterialCardView(this).apply {
                radius = dp(14).toFloat()
                setCardBackgroundColor(Color.parseColor("#1E293B"))
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = Color.parseColor("#1E3A5F")
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also { it.bottomMargin = dp(8) }
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            val row1 = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val badge = TextView(this).apply {
                text = " #${index + 1} "
                setTextColor(Color.parseColor("#93C5FD"))
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#1E3A8A"))
                setPadding(dp(6), dp(2), dp(6), dp(2))
            }
            val sym = TextView(this).apply {
                text = "  ${a.symbol}"
                setTextColor(Color.parseColor("#F8FAFC"))
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val chColor = when {
                a.error != null -> Color.parseColor("#F87171")
                a.changeH24 == null -> Color.parseColor("#94A3B8")
                a.changeH24 >= 0 -> Color.parseColor("#34D399")
                else -> Color.parseColor("#F87171")
            }
            val price = TextView(this).apply {
                text = if (a.error != null) "—" else a.formatPrice()
                setTextColor(Color.parseColor("#F8FAFC"))
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val chg = TextView(this).apply {
                text = if (a.error != null) "ERR" else a.formatChange()
                setTextColor(chColor)
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(10), 0, 0, 0)
            }
            row1.addView(badge)
            row1.addView(sym)
            row1.addView(price)
            row1.addView(chg)

            val name = TextView(this).apply {
                text = a.name.ifBlank { " " }
                setTextColor(Color.parseColor("#64748B"))
                textSize = 11f
                setPadding(0, dp(2), 0, 0)
            }
            val meta = TextView(this).apply {
                text = if (a.error != null) {
                    a.error
                } else {
                    "Vol 24h  $${String.format(Locale.US, "%,.0f", a.volumeH24)}" +
                        if (a.liquidityUsd > 0) {
                            "  ·  Liq $${String.format(Locale.US, "%,.0f", a.liquidityUsd)}"
                        } else ""
                }
                setTextColor(Color.parseColor("#64748B"))
                textSize = 11f
                setPadding(0, dp(4), 0, 0)
            }
            val contract = TextView(this).apply {
                text = a.address.ifBlank { "—" }
                setTextColor(Color.parseColor("#60A5FA"))
                textSize = 10f
                setTextIsSelectable(true)
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(0, dp(4), 0, 0)
            }
            col.addView(row1)
            col.addView(name)
            col.addView(meta)
            col.addView(contract)
            card.addView(col)
            container.addView(card)
        }
    }

    private fun runBlockchainSelfTest() {
        setChainBusy(true)
        binding.chainStatus.setText(R.string.chain_status_testing)
        binding.chainStatus.setTextColor(Color.parseColor("#9CA3AF"))
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Result.success(RobinhoodChainTracker.runSelfTests())
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            setChainBusy(false)
            result.fold(
                onSuccess = { tests ->
                    val lines = tests.map { t ->
                        val flag = if (t.ok) "PASS" else "FAIL"
                        "[$flag] ${t.name} — ${t.detail}"
                    }
                    val failed = tests.count { !it.ok }
                    val summary = "TOTAL ${tests.size - failed} passed, $failed failed of ${tests.size}"
                    binding.chainTestLog.text = (lines + summary).joinToString("\n")
                    binding.chainStatus.text =
                        if (failed == 0) "Self-test passed." else "Self-test: $failed failure(s)."
                    binding.chainStatus.setTextColor(
                        Color.parseColor(if (failed == 0) "#10B981" else "#EF4444")
                    )
                },
                onFailure = { e ->
                    binding.chainTestLog.text = "Self-test crashed: ${e.message}"
                    binding.chainStatus.text = "Self-test crashed: ${e.message}"
                    binding.chainStatus.setTextColor(Color.parseColor("#EF4444"))
                },
            )
        }
    }

    private fun addChainContract() {
        val raw = binding.chainContractEntry.text?.toString()?.trim().orEmpty()
        try {
            val addr = RobinhoodChainTracker.addCustom(raw)
            binding.chainContractEntry.setText("")
            chainCategoryId = RobinhoodChainTracker.CAT_CUSTOM
            val labels = RobinhoodChainTracker.categoryLabels()
            val idx = labels.indexOf(RobinhoodChainTracker.categoryLabelForId(chainCategoryId))
            if (idx >= 0) binding.chainCategorySpinner.setSelection(idx)
            binding.chainCategoryDesc.text =
                RobinhoodChainTracker.categoryDescription(chainCategoryId)
            binding.chainStatus.text = getString(R.string.chain_added, addr)
            binding.chainStatus.setTextColor(Color.parseColor("#10B981"))
            refreshBlockchain()
        } catch (e: Exception) {
            binding.chainStatus.text = e.message ?: getString(R.string.chain_bad_contract)
            binding.chainStatus.setTextColor(Color.parseColor("#EF4444"))
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
        private const val SECTION_CONVERT = 0
        private const val SECTION_TRAVEL = 1
        private const val SECTION_WEIGHT = 2
        private const val SECTION_TEMP = 3
        private const val SECTION_SPEC = 4
        private const val SECTION_CHAIN = 5
        private const val SECTION_SETTINGS = 6
    }
}
