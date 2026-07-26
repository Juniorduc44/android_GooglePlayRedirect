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
    private var exchangeRate: Double = FALLBACK_RATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versionLabel.text = "v${BuildConfig.VERSION_NAME}"
        binding.rateInfoLabel.text = getString(R.string.fetching_rate)

        binding.convertButton.setOnClickListener { convertCurrency() }
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
            exchangeRate = rate
            updateRateLabel(live)
        }
    }

    private fun updateRateLabel(live: Boolean) {
        val base = getString(R.string.rate_fmt, exchangeRate)
        binding.rateInfoLabel.setTextColor(Color.parseColor("#9CA3AF"))
        binding.rateInfoLabel.text = if (live) {
            base
        } else {
            "$base · ${getString(R.string.using_fallback)}"
        }
    }

    private fun convertCurrency() {
        val raw = binding.amountEntry.text?.toString()?.trim().orEmpty()

        if (raw.isEmpty()) {
            binding.resultLabel.text = getString(R.string.default_result)
            binding.resultLabel.setTextColor(Color.parseColor("#3B82F6"))
            binding.rateInfoLabel.text = getString(R.string.enter_amount)
            binding.rateInfoLabel.setTextColor(Color.parseColor("#EF4444"))
            return
        }

        val phpAmount = raw.toDoubleOrNull()
        if (phpAmount == null || phpAmount < 0.0) {
            binding.resultLabel.text = getString(R.string.invalid_input)
            binding.resultLabel.setTextColor(Color.parseColor("#EF4444"))
            binding.rateInfoLabel.text = getString(R.string.invalid_hint)
            binding.rateInfoLabel.setTextColor(Color.parseColor("#EF4444"))
            return
        }

        val usd = phpAmount * exchangeRate
        binding.resultLabel.text = String.format(Locale.US, "$%,.2f USD", usd)
        binding.resultLabel.setTextColor(Color.parseColor("#10B981"))
        binding.rateInfoLabel.text = getString(R.string.rate_fmt, exchangeRate)
        binding.rateInfoLabel.setTextColor(Color.parseColor("#9CA3AF"))
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
