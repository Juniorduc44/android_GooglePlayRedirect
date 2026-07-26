package com.juniorduc44.playredirect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.juniorduc44.playredirect.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Help / status screen + debug controls.
 *
 * Debug mode logs Play/market intents that reach this app and lets the user
 * save the log to any location via the system document picker (SAF).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val exportLogLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) {
            DebugLog.d(this, "EXPORT", "user cancelled save dialog")
            Toast.makeText(this, R.string.debug_export_cancelled, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        try {
            val text = DebugLog.buildExportText(this)
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: throw IllegalStateException("openOutputStream returned null")
            DebugLog.d(this, "EXPORT", "saved log to $uri (${text.length} chars)")
            Toast.makeText(this, R.string.debug_export_ok, Toast.LENGTH_LONG).show()
            refreshDebugPreview()
        } catch (t: Throwable) {
            DebugLog.e(this, "EXPORT", "failed to save log to $uri", t)
            Toast.makeText(
                this,
                getString(R.string.debug_export_fail, t.message ?: "error"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versionLabel.text = "v${BuildConfig.VERSION_NAME}"

        // --- Debug controls ---
        val debugOn = DebugPrefs.isDebugEnabled(this)
        binding.debugSwitch.isChecked = debugOn
        updateDebugPanelVisibility(debugOn)
        if (debugOn) {
            DebugLog.sessionHeader(this, "MainActivity.onCreate")
            DebugLog.logIntent(this, "MainActivity", intent, extrasNote = "launcher/open")
        }

        binding.debugSwitch.setOnCheckedChangeListener { _, isChecked ->
            DebugPrefs.setDebugEnabled(this, isChecked)
            updateDebugPanelVisibility(isChecked)
            if (isChecked) {
                DebugLog.sessionHeader(this, "debug enabled by user")
                DebugLog.d(this, "UI", "Debug mode ON")
                Toast.makeText(this, R.string.debug_enabled_toast, Toast.LENGTH_LONG).show()
            } else {
                // one last line before silence (enable briefly to write)
                DebugPrefs.setDebugEnabled(this, true)
                DebugLog.d(this, "UI", "Debug mode OFF (logging stops)")
                DebugPrefs.setDebugEnabled(this, false)
                Toast.makeText(this, R.string.debug_disabled_toast, Toast.LENGTH_SHORT).show()
            }
            refreshDebugPreview()
        }

        binding.saveDebugButton.setOnClickListener {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val name = "play-redirect-debug-$stamp.txt"
            DebugLog.d(this, "EXPORT", "opening SAF create document name=$name")
            exportLogLauncher.launch(name)
        }

        binding.clearDebugButton.setOnClickListener {
            DebugLog.clear(this)
            refreshDebugPreview()
            Toast.makeText(this, R.string.debug_cleared, Toast.LENGTH_SHORT).show()
        }

        binding.refreshDebugButton.setOnClickListener {
            refreshDebugPreview()
        }

        binding.testRedirectButton.setOnClickListener {
            val demo = Uri.parse("market://details?id=com.android.chrome")
            DebugLog.d(this, "UI", "Test redirect tapped demo=$demo")
            // Route through our RedirectActivity so debug path matches real usage
            val intent = Intent(this, RedirectActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = demo
            }
            startActivity(intent)
            binding.root.postDelayed({ refreshDebugPreview() }, 400)
        }

        binding.openAppSettingsButton.setOnClickListener {
            DebugLog.d(this, "UI", "Open app settings")
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
            startActivity(intent)
        }

        binding.openDefaultAppsButton.setOnClickListener {
            DebugLog.d(this, "UI", "Open default apps settings")
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            try {
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        refreshDebugPreview()
    }

    override fun onResume() {
        super.onResume()
        if (DebugPrefs.isDebugEnabled(this)) {
            DebugLog.d(this, "UI", "MainActivity.onResume")
            refreshDebugPreview()
        }
    }

    private fun updateDebugPanelVisibility(enabled: Boolean) {
        val vis = if (enabled) View.VISIBLE else View.GONE
        binding.debugActions.visibility = vis
        binding.debugPreviewCard.visibility = vis
        binding.debugScopeNote.visibility = View.VISIBLE // always show honest scope note
    }

    private fun refreshDebugPreview() {
        if (!DebugPrefs.isDebugEnabled(this)) {
            binding.debugPreview.text = getString(R.string.debug_preview_off)
            return
        }
        val all = DebugLog.readAll(this)
        // Show last ~8k chars so UI stays light
        binding.debugPreview.text = if (all.length > 8000) {
            "…\n" + all.takeLast(8000)
        } else {
            all
        }
    }
}
