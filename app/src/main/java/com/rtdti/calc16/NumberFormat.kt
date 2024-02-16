package com.rtdti.calc16

// import android.icu.number.NumberFormatter
// import android.icu.number.Precision
// import android.icu.util.ULocale
import androidx.compose.ui.text.AnnotatedString
import java.text.DecimalFormat
import kotlin.math.absoluteValue

interface StackFormatter {
    fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString
    fun makeEString(error: Double, epsilon: Double): String {
        return if (error.absoluteValue > epsilon) { " + ϵ" } else { "" } // FIXME
    }
    fun formatFrac(f: Frac, epsilon: Double, improper: Boolean): String {
        val eString = makeEString(f.err, epsilon * epsilon)
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
}
object StackFormatFloat : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        return AnnotatedString(CalcMath.floatString(value))
    }
}

object StackFormatHex : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        val truncated = value.toLong()
        val error = value - truncated
        val epsilon = formatState.epsilon
        val eString = makeEString (error, epsilon * epsilon)
        return AnnotatedString(String.format("%s = 0x%x%s", CalcMath.floatString(value), truncated, eString))
    }
}

object StackFormatImproper : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        val epsilon = formatState.epsilon
        val f = CalcMath.double2frac(value, 1/epsilon)
        return AnnotatedString(formatFrac(f, epsilon, true))
    }
}

object StackFormatMixImperial : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        val epsilon = formatState.epsilon
        val f = CalcMath.double2imperial(value, epsilon)
        return AnnotatedString(formatFrac(f, epsilon, false))
    }
}

object StackFormatFix : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        val decimalPlaces = formatState.decimalPlaces
        return AnnotatedString(String.format(String.format("%%.%df", decimalPlaces), value))
    }
}

object StackFormatSci : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        val decimalPlaces = formatState.decimalPlaces
        return AnnotatedString(String.format(String.format("%%.%de", decimalPlaces), value))
    }
}

object StackFormatPrime : StackFormatter {
    var superscriptFontSizeInt = 0
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        return AnnotatedString(String.format("%s = ", CalcMath.floatString(value))) +
            CalcMath.primeFactorAnnotatedString(value.toLong(), superscriptFontSizeInt)
    }
}

object StackFormatTime : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        val hr = Math.floor(value)
        val min = 60 * (value - hr)
        return AnnotatedString(String.format("%s = %s", CalcMath.floatString(value), CalcMath.timeString(value)))
    }
}

enum class NumberFormat { FLOAT, HEX, IMPROPER, MIXIMPERIAL, PRIME, FIX, SCI, TIME;
    fun formatter(): StackFormatter {
        return when (this) {
            FLOAT -> StackFormatFloat
            HEX -> StackFormatHex
            IMPROPER -> StackFormatImproper
            MIXIMPERIAL -> StackFormatMixImperial
            PRIME -> StackFormatPrime
            FIX -> StackFormatFix
            SCI -> StackFormatSci
            TIME -> StackFormatTime
        }
    }
    fun parser(str: String): Double {
        return if (this == NumberFormat.HEX) {
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
