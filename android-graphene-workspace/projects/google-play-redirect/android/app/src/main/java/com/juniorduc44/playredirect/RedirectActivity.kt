package com.juniorduc44.playredirect

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Handles market:// and play.google.com VIEW intents and immediately opens
 * an HTTPS equivalent in the default browser. No UI; finishes right away.
 *
 * When debug mode is enabled, every inbound intent and outcome is written to
 * the on-device debug log (exportable from MainActivity).
 */
class RedirectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DebugPrefs.isDebugEnabled(this)) {
            DebugLog.sessionHeader(this, "RedirectActivity.onCreate")
        }
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        DebugLog.logIntent(this, "RedirectActivity", intent, extrasNote="inbound")

        val uri = intent?.data
        val browserUrl = PlayLinkResolver.toBrowserUrl(uri)

        if (browserUrl.isNullOrBlank()) {
            DebugLog.logRedirectResult(
                this, uri, null, "UNHANDLED",
                "could not map to browser URL"
            )
            Toast.makeText(this, R.string.error_unhandled_link, Toast.LENGTH_LONG).show()
            return
        }

        DebugLog.d(this, "REDIRECT", "resolved browserUrl=$browserUrl")

        val view = Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(Intent.createChooser(view, getString(R.string.open_with_browser)))
            DebugLog.logRedirectResult(this, uri, browserUrl, "OPENED_CHOOSER")
        } catch (e: ActivityNotFoundException) {
            DebugLog.e(this, "REDIRECT", "no browser for $browserUrl", e)
            DebugLog.logRedirectResult(this, uri, browserUrl, "NO_BROWSER")
            Toast.makeText(this, R.string.error_no_browser, Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            DebugLog.e(this, "REDIRECT", "security block opening $browserUrl", e)
            DebugLog.logRedirectResult(this, uri, browserUrl, "SECURITY")
            Toast.makeText(this, R.string.error_security, Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            DebugLog.e(this, "REDIRECT", "unexpected failure opening $browserUrl", t)
            DebugLog.logRedirectResult(this, uri, browserUrl, "ERROR", t.message ?: "")
            Toast.makeText(this, R.string.error_security, Toast.LENGTH_LONG).show()
        }
    }
}
