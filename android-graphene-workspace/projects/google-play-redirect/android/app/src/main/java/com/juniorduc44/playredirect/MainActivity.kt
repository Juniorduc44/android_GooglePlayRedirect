package com.juniorduc44.playredirect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.juniorduc44.playredirect.databinding.ActivityMainBinding

/**
 * Help / status screen. Explains GrapheneOS usage and how this redirect helps
 * when apps open Play Store links. Does not replace sandboxed Play Services
 * for full in-app Google Sign-In.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versionLabel.text = "v${BuildConfig.VERSION_NAME}"

        binding.testRedirectButton.setOnClickListener {
            // Simulate a market:// details link through our resolver path
            val demo = Uri.parse("market://details?id=com.android.chrome")
            val https = PlayLinkResolver.toBrowserUrl(demo)
            if (https != null) {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(https)).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                )
            }
        }

        binding.openAppSettingsButton.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
            startActivity(intent)
        }

        binding.openDefaultAppsButton.setOnClickListener {
            // Opens system default-apps settings when available
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            try {
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }
}
