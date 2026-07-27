package com.juniorduc44.phpusdconverter

import android.content.Context
import android.util.TypedValue
import android.widget.TextView

/** Persisted result text size for Convert + Travel result labels. */
object ResultTextPrefs {
    private const val PREFS = "php_usd_prefs"
    private const val KEY = "result_text_size"

    enum class Size(val key: String, val mainSp: Float, val fxSp: Float) {
        SMALL("small", 18f, 12f),
        MEDIUM("medium", 24f, 14f),
        LARGE("large", 32f, 16f),
        EXTRA_LARGE("xlarge", 40f, 18f);

        companion object {
            fun fromKey(key: String?): Size =
                entries.firstOrNull { it.key == key } ?: LARGE
        }
    }

    fun get(context: Context): Size {
        val k = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, Size.LARGE.key)
        return Size.fromKey(k)
    }

    fun set(context: Context, size: Size) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, size.key)
            .apply()
    }

    fun apply(main: TextView, fx: TextView?, size: Size) {
        main.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.mainSp)
        fx?.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.fxSp)
    }
}
