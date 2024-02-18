package com.rtdti.calc16

// import android.icu.number.NumberFormatter
// import android.icu.number.Precision
// import android.icu.util.ULocale
import androidx.compose.ui.text.AnnotatedString
import java.text.DecimalFormat
import kotlin.math.absoluteValue

enum class NumberFormat { FLOAT, HEX, IMPROPER, MIXIMPERIAL, PRIME, FIX, SCI, TIME }

class Formatter(val formatState: CalcViewModel.FormatState, val superscriptFontSizeInt: Int) {
    private fun makeEString(error: Double, epsilon: Double): String {
        return if (error.absoluteValue > epsilon) { " + ϵ" } else { "" } // FIXME
    }
    private fun formatFrac(f: Frac, improper: Boolean): String {
        val eString = makeEString(f.err, formatState.epsilon * formatState.epsilon)
        var n = f.num
        var d = f.denom
        if (d < 0) { // Force d positive
            d = -d
            n = -n
        }
        var sign = ""
        if (n < 0) { // Force n positive
            sign = "-"
            n = -n
        }
        var w = 0L
        if (!improper) {
            w = n / d
            n = n % d
        }
        if (n == 0L) {
            d = 1L
        }
        if (d == 1L) {
            return String.format("%s%d%s", sign, if (improper) n else w, eString)
        }
        if (w == 0L) {
            return String.format("%s%d / %d%s", sign, n, d, eString)
        }
        return String.format("%s%d - %d / %d%s", sign, w, n, d, eString)
    }
    private fun formatFloat(value: Double): AnnotatedString {
        return AnnotatedString(CalcMath.floatString(value))
    }
    private fun formatHex(value: Double): AnnotatedString {
        val truncated = value.toLong()
        val error = value - truncated
        val eString = makeEString(error, formatState.epsilon)
        return AnnotatedString(String.format("%s = 0x%x%s", CalcMath.floatString(value), truncated, eString))
    }
    private fun formatImproper(value: Double): AnnotatedString {
        val f = CalcMath.double2frac(value, 1/formatState.epsilon)
        return AnnotatedString(formatFrac(f, true))
    }
    private fun formatMixImperial(value: Double): AnnotatedString {
        val f = CalcMath.double2imperial(value, formatState.epsilon)
        return AnnotatedString(formatFrac(f, false))
    }
    private fun formatFix(value: Double): AnnotatedString {
        return AnnotatedString(String.format(String.format("%%.%df", formatState.decimalPlaces), value))
    }
    private fun formatSci(value: Double): AnnotatedString {
        return AnnotatedString(String.format(String.format("%%.%de", formatState.decimalPlaces), value))
    }
    private fun formatPrime(value: Double): AnnotatedString {
        return AnnotatedString(String.format("%s = ", CalcMath.floatString(value))) +
                CalcMath.primeFactorAnnotatedString(value.toLong(), superscriptFontSizeInt)
    }
    private fun formatTime(value: Double): AnnotatedString {
        return AnnotatedString(String.format("%s = %s", CalcMath.floatString(value), CalcMath.timeString(value)))
    }
    fun format(value: Double): AnnotatedString {
        return when (formatState.numberFormat) {
            NumberFormat.FLOAT -> formatFloat(value)
            NumberFormat.HEX -> formatHex(value)
            NumberFormat.IMPROPER -> formatImproper(value)
            NumberFormat.MIXIMPERIAL -> formatMixImperial(value)
            NumberFormat.PRIME -> formatPrime(value)
            NumberFormat.FIX -> formatFix(value)
            NumberFormat.SCI -> formatSci(value)
            NumberFormat.TIME -> formatTime(value)
        }
    }
    fun parser(str: String): Double {
        return if (formatState.numberFormat == NumberFormat.HEX) {
            val ul = str.toULong(radix = 16)
            if (ul and 0x8000000000000000UL != 0UL) {
                -1.0 - (ul xor 0xffffffffffffffffUL).toDouble()
            } else {
                ul.toDouble()
            }
        } else {
            str.toDouble()
        }
    }
}
