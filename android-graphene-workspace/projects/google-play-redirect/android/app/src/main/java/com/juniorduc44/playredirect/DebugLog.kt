package com.juniorduc44.playredirect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * File-backed debug logger for Play Redirect.
 *
 * Scope (honest limits):
 * - Can log every intent / redirect this app receives and processes.
 * - Cannot silently monitor Google Sign-In inside *other* apps without root,
 *   Accessibility, or privileged logcat — GrapheneOS default forbids that.
 * - When debug is on, dump maximum metadata about what *does* hit us so you
 *   can see which app opened a Play link and what we did with it.
 */
object DebugLog {

    private const val DIR = "debug"
    private const val FILE = "play-redirect-debug.log"
    private const val MAX_BYTES = 2 * 1024 * 1024 // rotate after ~2 MiB
    private val lock = ReentrantLock()
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)

    fun logFile(context: Context): File {
        val dir = File(context.applicationContext.filesDir, DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE)
    }

    fun clear(context: Context) {
        lock.withLock {
            val f = logFile(context)
            if (f.exists()) f.writeText("")
        }
        d(context, "SYSTEM", "Log cleared by user")
    }

    fun readAll(context: Context): String = lock.withLock {
        val f = logFile(context)
        if (!f.exists()) return@withLock "(empty — no debug events yet)\n"
        f.readText()
    }

    fun d(context: Context, tag: String, message: String) {
        if (!DebugPrefs.isDebugEnabled(context)) return
        append(context, "D", tag, message)
    }

    fun i(context: Context, tag: String, message: String) {
        // Important path events still only when debug on (user-controlled)
        if (!DebugPrefs.isDebugEnabled(context)) return
        append(context, "I", tag, message)
    }

    fun e(context: Context, tag: String, message: String, t: Throwable? = null) {
        if (!DebugPrefs.isDebugEnabled(context)) return
        val extra = t?.let { "\n  exception=${it.javaClass.name}: ${it.message}" } ?: ""
        append(context, "E", tag, message + extra)
    }

    fun sessionHeader(context: Context, reason: String) {
        if (!DebugPrefs.isDebugEnabled(context)) return
        val pm = context.packageManager
        val appInfo = try {
            pm.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
        append(
            context,
            "I",
            "SESSION",
            buildString {
                appendLine("=== debug session ($reason) ===")
                appendLine("app=${context.packageName}")
                appendLine("versionName=${BuildConfig.VERSION_NAME} versionCode=${BuildConfig.VERSION_CODE}")
                appendLine("debugEnabled=true")
                appendLine("sdk=${Build.VERSION.SDK_INT} release=${Build.VERSION.RELEASE}")
                appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("fingerprint=${Build.FINGERPRINT}")
                appendLine("note=This log only includes events delivered to Play Redirect.")
                appendLine("note=System-wide Google Sign-In in other apps is NOT visible without root/adb logcat.")
                if (appInfo != null) {
                    appendLine("firstInstall=${appInfo.firstInstallTime} lastUpdate=${appInfo.lastUpdateTime}")
                }
            }.trimEnd()
        )
    }

    /**
     * Full dump of an Intent useful for diagnosing who tried to open Play / market.
     */
    fun logIntent(
        context: Context,
        tag: String,
        intent: Intent?,
        extrasNote: String = ""
    ) {
        if (!DebugPrefs.isDebugEnabled(context)) return
        if (intent == null) {
            d(context, tag, "intent=null $extrasNote")
            return
        }
        val sb = StringBuilder()
        sb.appendLine("intent dump $extrasNote")
        sb.appendLine("  action=${intent.action}")
        sb.appendLine("  data=${intent.dataString}")
        sb.appendLine("  type=${intent.type}")
        sb.appendLine("  flags=0x${Integer.toHexString(intent.flags)}")
        sb.appendLine("  categories=${intent.categories}")
        sb.appendLine("  component=${intent.component}")
        sb.appendLine("  package=${intent.`package`}")
        sb.appendLine("  scheme=${intent.scheme}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // referrer often shows which app launched us
            try {
                // Activity context preferred; callers pass activity when possible
                if (context is android.app.Activity) {
                    sb.appendLine("  activity.referrer=${context.referrer}")
                    sb.appendLine("  activity.callingPackage=${context.callingPackage}")
                    sb.appendLine("  activity.callingActivity=${context.callingActivity}")
                }
            } catch (_: Exception) {
                // ignore
            }
        }
        intent.extras?.let { bundle ->
            sb.appendLine("  extras (${bundle.keySet().size} keys):")
            for (key in bundle.keySet().sorted()) {
                sb.appendLine("    $key=${safeExtra(bundle, key)}")
            }
        } ?: sb.appendLine("  extras=null")
        intent.data?.let { uri ->
            sb.appendLine("  uri.scheme=${uri.scheme} host=${uri.host} path=${uri.path}")
            sb.appendLine("  uri.query=${uri.encodedQuery}")
            try {
                for (name in uri.queryParameterNames) {
                    sb.appendLine("    query.$name=${uri.getQueryParameter(name)}")
                }
            } catch (_: Exception) {
                // some malformed URIs throw
            }
        }
        append(context, "D", tag, sb.toString().trimEnd())
    }

    fun logRedirectResult(
        context: Context,
        input: Uri?,
        browserUrl: String?,
        outcome: String,
        detail: String = ""
    ) {
        if (!DebugPrefs.isDebugEnabled(context)) return
        d(
            context,
            "REDIRECT",
            "input=$input → browserUrl=$browserUrl | outcome=$outcome ${detail.trim()}".trim()
        )
    }

    private fun safeExtra(bundle: Bundle, key: String): String {
        val sensitive = listOf("password", "token", "secret", "auth", "cookie")
        if (sensitive.any { key.contains(it, ignoreCase = true) }) {
            return "<redacted>"
        }
        return try {
            val v = bundle.get(key)
            when (v) {
                null -> "null"
                is Bundle -> "Bundle(${v.keySet()})"
                is ByteArray -> "byte[${v.size}]"
                else -> v.toString().take(500)
            }
        } catch (t: Throwable) {
            "<unreadable: ${t.message}>"
        }
    }

    private fun append(context: Context, level: String, tag: String, message: String) {
        lock.withLock {
            val f = logFile(context)
            rotateIfNeeded(f)
            val line = "${timeFmt.format(Date())} $level/$tag: $message\n"
            f.appendText(line)
        }
    }

    private fun rotateIfNeeded(f: File) {
        if (f.exists() && f.length() > MAX_BYTES) {
            val bak = File(f.parentFile, "$FILE.1")
            if (bak.exists()) bak.delete()
            f.renameTo(bak)
            f.writeText("")
        }
    }

    /** Full export body with header for the user-chosen save location. */
    fun buildExportText(context: Context): String {
        val body = readAll(context)
        return buildString {
            appendLine("Play Redirect debug export")
            appendLine("exportedAt=${timeFmt.format(Date())}")
            appendLine("package=${context.packageName}")
            appendLine("versionName=${BuildConfig.VERSION_NAME}")
            appendLine("versionCode=${BuildConfig.VERSION_CODE}")
            appendLine("debugEnabled=${DebugPrefs.isDebugEnabled(context)}")
            appendLine("---")
            append(body)
            if (!body.endsWith("\n")) appendLine()
        }
    }
}
