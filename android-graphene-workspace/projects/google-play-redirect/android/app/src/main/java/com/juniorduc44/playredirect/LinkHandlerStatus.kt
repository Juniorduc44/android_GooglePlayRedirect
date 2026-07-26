package com.juniorduc44.playredirect

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build

/**
 * Diagnostics: which packages can handle market:// and play.google.com VIEW
 * intents, and whether this app is among them / preferred.
 *
 * Note: apps that launch Play with an *explicit* component/package
 * (setPackage("com.android.vending")) never hit us — that is not a
 * permission we can grant ourselves without root.
 */
object LinkHandlerStatus {

    data class Report(
        val marketHandlers: List<String>,
        val playWebHandlers: List<String>,
        val weHandleMarket: Boolean,
        val weHandlePlayWeb: Boolean,
        val preferredMarket: String?,
        val preferredPlayWeb: String?,
        val playStoreInstalled: Boolean,
        val summary: String
    )

    fun build(context: Context): Report {
        val pm = context.packageManager
        val self = context.packageName

        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.android.chrome")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val playIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.android.chrome")
        ).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        val marketList = query(pm, marketIntent)
        val playList = query(pm, playIntent)
        val marketNames = marketList.map { label(pm, it) }
        val playNames = playList.map { label(pm, it) }

        val weMarket = marketList.any { it.activityInfo.packageName == self }
        val wePlay = playList.any { it.activityInfo.packageName == self }

        val prefMarket = preferred(pm, marketIntent)?.let { label(pm, it) }
        val prefPlay = preferred(pm, playIntent)?.let { label(pm, it) }

        val playStoreInstalled = try {
            pm.getPackageInfo("com.android.vending", 0)
            true
        } catch (_: Exception) {
            false
        }

        val summary = buildString {
            appendLine("=== link handler status ===")
            appendLine("thisApp=$self")
            appendLine("weRegisteredForMarket=$weMarket weRegisteredForPlayWeb=$wePlay")
            appendLine("playStorePackageInstalled=$playStoreInstalled (com.android.vending)")
            appendLine("handlers market:// (${marketNames.size}): ${marketNames.joinToString()}")
            appendLine("handlers play.google.com (${playNames.size}): ${playNames.joinToString()}")
            appendLine("preferred market:// = ${prefMarket ?: "(none / always ask)"}")
            appendLine("preferred play.google.com = ${prefPlay ?: "(none / always ask)"}")
            appendLine()
            appendLine("HOW OTHER APPS REACH US:")
            appendLine("1) Other app must fire an *implicit* VIEW intent (market:// or https://play.google.com/...).")
            appendLine("2) Android must resolve that intent to Play Redirect (chooser → Always, or Open by default).")
            appendLine("3) If another app is preferred (Play Store, browser), we never run — not a missing permission.")
            appendLine("4) If an app uses setPackage(com.android.vending) / explicit component, we CANNOT intercept (OS rule).")
            appendLine("5) In-app Google Sign-In uses GMS APIs, not market links — use GrapheneOS sandboxed Play for that.")
            if (playStoreInstalled && prefMarket?.contains("vending", true) == true ||
                prefMarket?.contains("Play", true) == true
            ) {
                appendLine()
                appendLine("HINT: Play Store looks preferred for market:// — clear its defaults or set Play Redirect as Always.")
            }
            if (!weMarket || !wePlay) {
                appendLine()
                appendLine("WARNING: this app is not showing in the system handler list — reinstall or check intent filters.")
            }
        }.trimEnd()

        return Report(
            marketHandlers = marketNames,
            playWebHandlers = playNames,
            weHandleMarket = weMarket,
            weHandlePlayWeb = wePlay,
            preferredMarket = prefMarket,
            preferredPlayWeb = prefPlay,
            playStoreInstalled = playStoreInstalled,
            summary = summary
        )
    }

    private fun query(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, flags)
        }
    }

    private fun preferred(pm: PackageManager, intent: Intent): ResolveInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
    }

    private fun label(pm: PackageManager, info: ResolveInfo): String {
        val pkg = info.activityInfo.packageName
        val name = try {
            info.loadLabel(pm).toString()
        } catch (_: Exception) {
            pkg
        }
        return "$name ($pkg)"
    }
}
