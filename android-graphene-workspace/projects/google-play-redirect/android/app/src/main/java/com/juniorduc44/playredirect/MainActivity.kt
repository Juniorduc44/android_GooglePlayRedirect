package com.juniorduc44.playredirect

import android.content.Intent
import android.net.Uri
import android.os.Build
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
 * Help / diagnostics / debug.
 *
 * External apps only reach RedirectActivity when Android resolves Play/market
 * VIEW intents to this package — that is link-handler configuration, not an
 * extra runtime permission we can request.
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
                refreshHandlerStatus()
                Toast.makeText(this, R.string.debug_enabled_toast, Toast.LENGTH_LONG).show()
            } else {
                DebugPrefs.setDebugEnabled(this, true)
                DebugLog.d(this, "UI", "Debug mode OFF (logging stops)")
                DebugPrefs.setDebugEnabled(this, false)
                Toast.makeText(this, R.string.debug_disabled_toast, Toast.LENGTH_SHORT).show()
            }
            refreshDebugPreview()
        }

        binding.saveDebugButton.setOnClickListener {
            // Include latest handler status in the export
            refreshHandlerStatus()
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

        binding.checkHandlersButton.setOnClickListener {
            refreshHandlerStatus()
            Toast.makeText(this, "Handler status updated", Toast.LENGTH_SHORT).show()
        }

        binding.openByDefaultButton.setOnClickListener {
            openOpenByDefaultSettings()
        }

        // System-resolved market:// — same class of intent other apps use
        binding.testExternalButton.setOnClickListener {
            val demo = Uri.parse("market://details?id=com.android.chrome")
            DebugLog.d(this, "UI", "Test EXTERNAL market:// VIEW (no setClass) demo=$demo")
            val intent = Intent(Intent.ACTION_VIEW, demo).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            try {
                startActivity(intent)
            } catch (t: Throwable) {
                DebugLog.e(this, "UI", "external market VIEW failed", t)
                Toast.makeText(this, t.message ?: "Failed", Toast.LENGTH_LONG).show()
            }
            binding.root.postDelayed({
                refreshHandlerStatus()
                refreshDebugPreview()
            }, 500)
        }

        // Always works: explicit component inside our app
        binding.testRedirectButton.setOnClickListener {
            val demo = Uri.parse("market://details?id=com.android.chrome")
            DebugLog.d(this, "UI", "Test IN-APP RedirectActivity demo=$demo")
            startActivity(
                Intent(this, RedirectActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = demo
                }
            )
            binding.root.postDelayed({ refreshDebugPreview() }, 400)
        }

        binding.openAppSettingsButton.setOnClickListener {
            DebugLog.d(this, "UI", "Open app settings")
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
            )
        }

        binding.openDefaultAppsButton.setOnClickListener {
            DebugLog.d(this, "UI", "Open default apps settings")
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        refreshHandlerStatus()
        refreshDebugPreview()
    }

    override fun onResume() {
        super.onResume()
        refreshHandlerStatus()
        if (DebugPrefs.isDebugEnabled(this)) {
            DebugLog.d(this, "UI", "MainActivity.onResume")
            refreshDebugPreview()
        }
    }

    private fun openOpenByDefaultSettings() {
        DebugLog.d(this, "UI", "Open by default settings")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(
                    Intent(
                        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                )
                return
            }
        } catch (t: Throwable) {
            DebugLog.e(this, "UI", "ACTION_APP_OPEN_BY_DEFAULT_SETTINGS failed", t)
        }
        // Fallback: app details (user can open “Open by default” from there)
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }

    private fun refreshHandlerStatus() {
        val report = LinkHandlerStatus.build(this)
        binding.handlerStatusText.text = report.summary
        DebugLog.d(this, "HANDLERS", report.summary)
    }

    private fun updateDebugPanelVisibility(enabled: Boolean) {
        val vis = if (enabled) View.VISIBLE else View.GONE
        binding.debugActions.visibility = vis
        binding.debugPreviewCard.visibility = vis
        binding.debugScopeNote.visibility = View.VISIBLE
    }

    private fun refreshDebugPreview() {
        if (!DebugPrefs.isDebugEnabled(this)) {
            binding.debugPreview.text = getString(R.string.debug_preview_off)
            return
        }
        val all = DebugLog.readAll(this)
        binding.debugPreview.text = if (all.length > 8000) {
            "…\n" + all.takeLast(8000)
        } else {
            all
        }
    }
}
