package com.juniorduc44.phpusdconverter

import java.util.Locale
import kotlin.math.abs

/** Pure math + formatting for Spec tab (mirrors desktop `speculator/math_fmt.py`). */
object SpeculatorMath {

    fun parseNumber(raw: String?): Double? {
        if (raw == null) return null
        var s = raw.trim().lowercase(Locale.US)
            .replace(",", "")
            .replace("$", "")
            .replace(" ", "")
        if (s.isEmpty() || s == "." || s == "-" || s == "+") return null
        var mult = 1.0
        when {
            s.endsWith("b") -> {
                mult = 1e9
                s = s.dropLast(1)
            }
            s.endsWith("m") -> {
                mult = 1e6
                s = s.dropLast(1)
            }
            s.endsWith("k") -> {
                mult = 1e3
                s = s.dropLast(1)
            }
        }
        s = s.replace(Regex("[^0-9.eE+-]"), "")
        if (s.isEmpty() || s in setOf(".", "-", "+", "e", "E")) return null
        return try {
            s.toDouble() * mult
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun priceFromMcap(marketCap: Double, supply: Double): Double {
        require(supply != 0.0) { "supply" }
        return marketCap / supply
    }

    fun mcapFromPrice(price: Double, supply: Double): Double = price * supply

    fun itemsFromSpend(spent: Double, price: Double): Double {
        require(price != 0.0) { "price" }
        return spent / price
    }

    fun costForItems(price: Double, items: Double): Double = price * items

    fun valueAtTarget(holdings: Double, targetPrice: Double): Double =
        holdings * targetPrice

    fun avgCost(spent: Double, holdings: Double): Double {
        require(holdings != 0.0) { "holdings" }
        return spent / holdings
    }

    fun pnlAtTarget(holdings: Double, targetPrice: Double, spent: Double): Double =
        valueAtTarget(holdings, targetPrice) - spent

    fun formatMoney(value: Double): String {
        val av = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            av >= 1e12 -> String.format(Locale.US, "%s$%.3fT", sign, av / 1e12)
            av >= 1e9 -> String.format(Locale.US, "%s$%.3fB", sign, av / 1e9)
            av >= 1e6 -> String.format(Locale.US, "%s$%.3fM", sign, av / 1e6)
            av >= 1e3 -> String.format(Locale.US, "%s$%,.2f", sign, av)
            av >= 1.0 -> String.format(Locale.US, "%s$%,.4f", sign, av)
            av >= 1e-4 -> String.format(Locale.US, "%s$%.6f", sign, av)
            av == 0.0 -> "$0"
            else -> String.format(Locale.US, "%s$%.4e", sign, av)
        }
    }

    fun formatQty(value: Double): String {
        val av = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            av >= 1e12 -> String.format(Locale.US, "%s%.3fT", sign, av / 1e12)
            av >= 1e9 -> String.format(Locale.US, "%s%.3fB", sign, av / 1e9)
            av >= 1e6 -> String.format(Locale.US, "%s%.3fM", sign, av / 1e6)
            av >= 1e3 -> String.format(Locale.US, "%s%,.2f", sign, av)
            av >= 1.0 -> String.format(Locale.US, "%s%,.4f", sign, av)
            av >= 1e-6 -> {
                val s = String.format(Locale.US, "%s%.8f", sign, av)
                s.trimEnd('0').trimEnd('.')
            }
            av == 0.0 -> "0"
            else -> String.format(Locale.US, "%s%.4e", sign, av)
        }
    }

    /** Compact string suitable for pasting into another field. */
    fun formatRaw(value: Double): String {
        return String.format(Locale.US, "%.12g", value)
    }
}
