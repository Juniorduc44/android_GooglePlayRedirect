package com.juniorduc44.playredirect

import android.content.Context

/** Shared preferences for debug mode toggle. */
object DebugPrefs {
    private const val PREFS = "play_redirect_prefs"
    private const val KEY_DEBUG = "debug_mode"

    fun isDebugEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEBUG, false)

    fun setDebugEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEBUG, enabled)
            .apply()
    }
}
