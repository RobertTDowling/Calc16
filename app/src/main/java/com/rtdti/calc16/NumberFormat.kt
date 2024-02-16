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

enum class FloatWay { NUMBER_FORMATTER, LAZY, BY_HAND }
val Double.mantissa get() = toBits() and 0x000fffffffffffff
val Double.exponent get() = toBits() and 0x7ff0000000000000 shr 52
object StackFormatFloat : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        val way = FloatWay.NUMBER_FORMATTER
        when(way) {
            FloatWay.BY_HAND -> {
                val mant = value.mantissa + 0x000fffffffffffff + 1
                val exp = value.exponent - 1023
                val str = String.format("%s = %x,%d", value.toString(), mant, exp)
                return AnnotatedString(str)
            }
            FloatWay.NUMBER_FORMATTER -> { // Can't Unit test
                /* Android NumberFormatter
                val str = NumberFormatter.withLocale(ULocale.US)// .withLocale(Locale.US)
                    .grouping(NumberFormatter.GroupingStrategy.OFF)
                    .precision(Precision.unlimited())
                    .format(value).toString()
                 */
                lateinit var str: String
                if (value.absoluteValue > 1e16 || value.absoluteValue > 0 && value.absoluteValue < 1e-6 ) {
                    // Punt on big and small values (but not zero)
                    str = value.toString()
                } else {
                    val nf = DecimalFormat.getInstance()
                    nf.maximumFractionDigits = 16
                    nf.maximumIntegerDigits = 16
                    nf.isGroupingUsed = false
                    str = nf.format(value)
                }
                return AnnotatedString(str)
            }
            FloatWay.LAZY -> {
                val intPart = value.toLong()
                if (intPart.toDouble() == value) { // Format as an Int if we can
                    return AnnotatedString(intPart.toString())
                }
                return AnnotatedString(value.toString())
            }
        }
    }
}

object StackFormatHex : StackFormatter {
    override fun format(value: Double, formatState: CalcViewModel.FormatState): AnnotatedString {
        val truncated = value.toLong()
        val error = value - truncated
        val epsilon = formatState.epsilon
        val eString = makeEString (error, epsilon * epsilon)
        return AnnotatedString(String.format("0x%x%s", truncated, eString))
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
        return CalcMath.primeFactorAnnotatedString(value.toLong(), superscriptFontSizeInt)
    }
}

enum class NumberFormat { FLOAT, HEX, IMPROPER, MIXIMPERIAL, PRIME, FIX, SCI;
    fun formatter(): StackFormatter {
        return when (this) {
            FLOAT -> StackFormatFloat
            HEX -> StackFormatHex
            IMPROPER -> StackFormatImproper
            MIXIMPERIAL -> StackFormatMixImperial
            PRIME -> StackFormatPrime
            FIX -> StackFormatFix
            SCI -> StackFormatSci
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
