package com.rtdti.calc16

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import kotlin.math.absoluteValue

data class StackEntry(val value: Double)

class FormatParameters() {
    val epsilon = mutableStateOf(1e-4)
    val decimalPlaces = mutableStateOf(2)
    val superscriptFontSizeInt = mutableStateOf(0)
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

enum class StackFormat { FLOAT, HEX, IMPROPER, MIXIMPERIAL, PRIME, FIX, SCI;
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

class Stack() {
    private val entries = mutableStateListOf<StackEntry>()
    fun entry(depth: Int) : StackEntry {
        if (hasDepth(depth)) {
            return entries[depth]
        }
        throw IndexOutOfBoundsException()
    }
    fun depthGet() : Int { return entries.size }
    fun push(x: Double) {
        entries.add(0, StackEntry(x))
    }
    fun pop(): Double {
        return entries.removeAt(0).value
    }
    fun pick(index: Int) {
        push(entries[index].value)
    }
    fun hasDepth(d: Int): Boolean {
        return entries.size >= d
    }
    fun isEmpty(): Boolean { return !hasDepth(1)}
}

class Pad {
    private val pad = mutableStateOf("")
    fun get() : String { return pad.value }
    fun append(str: String) {
        pad.value = pad.value + str
    }
    fun clear() {
        pad.value = ""
    }

    fun backspace() {
        if (!isEmpty()) {
            pad.value = pad.value.substring(0, pad.value.length - 1)
        }
    }

    fun isEmpty(): Boolean {
        return pad.value.length == 0
    }
}

class Calc() {
    val stack = Stack()
    val pad = Pad()
    val format = mutableStateOf(StackFormat.FLOAT)
    val formatParameters = FormatParameters()
    fun formatGet() : StackFormat { return format.value }
    fun formatSet(fmt: StackFormat) { format.value = fmt }
    fun formatter(): StackFormatter { return format.value.formatter() }

    fun padAppend(str: String) { pad.append(str) }
    private fun padEnter() { // Copy pad to top of stack and clear pad
        if (pad.isEmpty()) {
            // Do nothing?
        } else {
            val x = try {
                if (formatGet() == StackFormat.HEX) {
                    pad.get().toLong(radix = 16).toDouble()
                } else {
                    pad.get().toDouble()
                }
            } catch (e: Exception) {
                0.0
            }
            stack.push(x)
            pad.clear()
        }
    }
    fun backspaceOrDrop() { // Combo backspace and drop
        if (pad.isEmpty()) {
            if (!stack.isEmpty()) {
                stack.pop()
            }
        } else {
            pad.backspace()
        }
    }
    fun enterOrDup() { // Combo enter and dup
        if (!pad.isEmpty()) {
            padEnter()
        } else {
            if (!stack.isEmpty()) {
                val a = stack.pop();
                stack.push(a)
                stack.push(a)
            }
        }
    }
    fun push(x: Double) {
        padEnter()
        stack.push(x)
    }
    fun swap() {
        padEnter();
        if (stack.hasDepth(2)) {
            val b = stack.pop()
            val a = stack.pop()
            push(b)
            push(a)
        }
    }
    fun binop(op: (Double, Double) -> Double) {
        padEnter()
        if (stack.hasDepth(2)) {
            val b = stack.pop()
            val a = stack.pop()
            stack.push(op(a, b))
        }
    }

    fun unop(op: (Double) -> Double) {
        padEnter()
        if (stack.hasDepth(1)) {
            val a = stack.pop()
            stack.push(op(a))
        }
    }
    fun pop1op(op: (Double) -> Unit) {
        padEnter()
        if (stack.hasDepth(1)) {
            val a = stack.pop()
            op(a)
        }
    }
}