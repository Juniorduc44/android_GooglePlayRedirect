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
 */
class RedirectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        val browserUrl = PlayLinkResolver.toBrowserUrl(uri)

        if (browserUrl.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_unhandled_link, Toast.LENGTH_LONG).show()
            return
        }

        val view = Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            // Prefer a real browser over looping back into this app
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(Intent.createChooser(view, getString(R.string.open_with_browser)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_no_browser, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.error_security, Toast.LENGTH_LONG).show()
        }
    }
}
