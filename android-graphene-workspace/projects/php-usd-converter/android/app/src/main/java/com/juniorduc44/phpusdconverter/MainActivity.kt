package com.juniorduc44.phpusdconverter

import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
    /** PHP per 1 unit of base; stored as USD per 1 PHP (same as API). */
    private var phpToUsdRate: Double = FALLBACK_RATE
    private var rateIsLive: Boolean = false
    /** true = PHP → USD; false = USD → PHP */
    private var phpToUsd: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versionLabel.text = "v${BuildConfig.VERSION_NAME}"
        binding.rateInfoLabel.text = getString(R.string.fetching_rate)
        applyDirectionLabels()

        binding.convertButton.setOnClickListener { convertCurrency() }
        binding.swapButton.setOnClickListener { swapDirection() }
        binding.amountEntry.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.amountEntry.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                convertCurrency()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            val (rate, live) = fetchLiveRate()
            phpToUsdRate = rate
            rateIsLive = live
            updateRateLabel()
        }
    }

    private fun swapDirection() {
        phpToUsd = !phpToUsd
        applyDirectionLabels()
        binding.rateInfoLabel.text = getString(R.string.fetching_rate)
        binding.rateInfoLabel.setTextColor(Color.parseColor("#9CA3AF"))
        // Same as startup: re-fetch live rate so swap also refreshes the quote
        lifecycleScope.launch {
            val (rate, live) = fetchLiveRate()
            phpToUsdRate = rate
            rateIsLive = live
            updateRateLabel()
            // Re-run conversion if there is already an amount
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
        }
    }

    private fun applyDirectionLabels() {
        if (phpToUsd) {
            binding.subtitleLabel.setText(R.string.subtitle_php_to_usd)
            binding.inputLabel.setText(R.string.amount_label_php)
            binding.amountEntry.setHint(R.string.amount_hint_php)
            binding.convertButton.setText(R.string.convert_to_usd)
        } else {
            binding.subtitleLabel.setText(R.string.subtitle_usd_to_php)
            binding.inputLabel.setText(R.string.amount_label_usd)
            binding.amountEntry.setHint(R.string.amount_hint_usd)
            binding.convertButton.setText(R.string.convert_to_php)
        }
    }

    private fun updateRateLabel() {
        binding.rateInfoLabel.setTextColor(Color.parseColor("#9CA3AF"))
        val base = if (phpToUsd) {
            getString(R.string.rate_php_to_usd, phpToUsdRate)
        } else {
            val usdToPhp = if (phpToUsdRate > 0) 1.0 / phpToUsdRate else 0.0
            getString(R.string.rate_usd_to_php, usdToPhp)
        }
        binding.rateInfoLabel.text = if (rateIsLive) {
            base
        } else {
            "$base · ${getString(R.string.using_fallback)}"
        }
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
            val usd = amount * phpToUsdRate
            binding.resultLabel.text = String.format(Locale.US, "$%,.2f USD", usd)
        } else {
            val php = if (phpToUsdRate > 0) amount / phpToUsdRate else 0.0
            binding.resultLabel.text = String.format(Locale.US, "₱%,.2f PHP", php)
        }
        binding.resultLabel.setTextColor(Color.parseColor("#10B981"))
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
                val rate = json.getJSONObject("rates").getDouble("USD")
                rate to true
            }
        } catch (_: Exception) {
            FALLBACK_RATE to false
        }
    }

    companion object {
        private const val RATE_URL = "https://api.exchangerate-api.com/v4/latest/PHP"
        private const val FALLBACK_RATE = 0.0175
    }
}
