package com.rtdti.calc16

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import kotlin.math.absoluteValue

class FormatParameters() {
    val epsilon = mutableStateOf(1e-4)
    val decimalPlaces = mutableStateOf(2)
    val superscriptFontSizeInt = mutableStateOf(0)
    val numberFormat = mutableStateOf(NumberFormat.FLOAT)
}

interface StackFormatter {
    fun format(value: Double, formatParameters: FormatParameters): AnnotatedString
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
    override fun format(value: Double, formatParameters: FormatParameters): AnnotatedString {
        return AnnotatedString(value.toString())
    }
}

object StackFormatHex : StackFormatter {
    override fun format(value: Double, formatParameters: FormatParameters): AnnotatedString {
        val truncated = value.toLong()
        val error = value - truncated
        val epsilon = formatParameters.epsilon.value
        val eString = makeEString (error, epsilon * epsilon)
        return AnnotatedString(String.format("0x%x%s", truncated, eString))
    }
}

object StackFormatImproper : StackFormatter {
    override fun format(value: Double, formatParameters: FormatParameters): AnnotatedString {
        val epsilon = formatParameters.epsilon.value
        val f = CalcMath.double2frac(value, epsilon)
        return AnnotatedString(formatFrac(f, epsilon, true))
    }
}

object StackFormatMixImperial : StackFormatter {
    override fun format(value: Double, formatParameters: FormatParameters): AnnotatedString {
        val epsilon = formatParameters.epsilon.value
        val f = CalcMath.double2imperial(value, epsilon)
        return AnnotatedString(formatFrac(f, epsilon, false))
    }
}

object StackFormatFix : StackFormatter {
    override fun format(value: Double, formatParameters: FormatParameters): AnnotatedString {
        val decimalPlaces = formatParameters.decimalPlaces.value
        return AnnotatedString(String.format(String.format("%%.%df", decimalPlaces), value))
    }
}

object StackFormatSci : StackFormatter {
    override fun format(value: Double, formatParameters: FormatParameters): AnnotatedString {
        val decimalPlaces = formatParameters.decimalPlaces.value
        return AnnotatedString(String.format(String.format("%%.%de", decimalPlaces), value))
    }
}

object StackFormatPrime : StackFormatter {
    override fun format(value: Double, formatParameters: FormatParameters): AnnotatedString {
        val superscriptFontSizeInt = formatParameters.superscriptFontSizeInt.value
        return CalcMath.primeFactorAnnotatedString(value.toLong(), superscriptFontSizeInt)
    }
}

enum class NumberFormat { FLOAT, HEX, IMPROPER, MIXIMPERIAL, PRIME, FIX, SCI;
    fun formatter() : StackFormatter {
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
}
