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
    val numberFormat = mutableStateOf(NumberFormat.FLOAT)
    /*
    fun set(zuper: Zuper) {
        epsilon.value = zuper.epsilon
        decimalPlaces.value = zuper.decimalPlaces
        numberFormat.value = NumberFormat.valueOf(zuper.numberFormat)
    }
    */
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

/*
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

class Calc() {
    val debugString = mutableStateOf("")
    val stack = Stack()
    val formatParameters = FormatParameters()
    val undoManager = UndoManager()
    fun formatGet() : NumberFormat { return formatParameters.numberFormat.value }
    fun formatSet(fmt: NumberFormat) { formatParameters.numberFormat.value = fmt }
    fun formatter(): StackFormatter { return formatParameters.numberFormat.value.formatter() }
    fun undoSave() { // Allocate new space and fill it with copy of current cached state
        undoManager.save(Zuper(0, 1, pad, stack, formatParameters))
        debugString.value = String.format("Undo+ size=%d", undoManager.history.size)
    }
    fun undoRestore(): Boolean { // Return true if undo stack is empty
        val zt = undoManager.restore()
        zt?.let {
            pad.set(zt)
            stack.set(zt)
            formatParameters.set(zt)
            debugString.value = String.format("Undo- size=%d", undoManager.history.size)
            return false
        }
        return true
    }

    fun padAppend(str: String) { pad.append(str) }

    private fun impliedEnter() { // Copy pad to top of stack and clear pad
        if (pad.isEmpty()) {
            // Do nothing?
        } else {
            val x = try {
                if (formatGet() == NumberFormat.HEX) {
                    pad.get().toLong(radix = 16).toDouble()
                } else {
                    pad.get().toDouble()
                }
            } catch (e: Exception) {
                0.0
            }
            stack.push(x)
            pad.clear()
            undoSave()
        }
    }
    fun backspaceOrDrop() { // Combo backspace and drop
        if (pad.isEmpty()) {
            if (!stack.isEmpty()) {
                stack.pop()
                undoSave()
            }
        } else {
            pad.backspace()
        }
    }
    fun enterOrDup() { // Combo enter and dup
        if (!pad.isEmpty()) {
            impliedEnter()
        } else {
            if (!stack.isEmpty()) {
                val a = stack.pop();
                stack.push(a)
                stack.push(a)
                undoSave()
            }
        }
    }
    fun pushConstant(x: Double) {
        impliedEnter()
        stack.push(x)
        undoSave()
    }
    fun swap() {
        impliedEnter()
        if (stack.hasDepth(2)) {
            val b = stack.pop()
            val a = stack.pop()
            stack.push(b)
            stack.push(a)
            undoSave()
        }
    }
    fun pick(index: Int) {
        impliedEnter()
        stack.pick(index)
        undoSave()
    }
    fun binop(op: (Double, Double) -> Double) {
        impliedEnter()
        if (stack.hasDepth(2)) {
            val b = stack.pop()
            val a = stack.pop()
            stack.push(op(a, b))
            undoSave()
        }
    }
    fun unop(op: (Double) -> Double) {
        impliedEnter()
        if (stack.hasDepth(1)) {
            val a = stack.pop()
            stack.push(op(a))
            undoSave()
        }
    }
    fun pop1op(op: (Double) -> Unit) {
        impliedEnter()
        if (stack.hasDepth(1)) {
            val a = stack.pop()
            op(a)
            undoSave()
        }
    }
}

class UndoManager {
    val TAG="UndoManager"
    val MAX_DEPTH = 20
    val history = mutableListOf<Zuper>()
    fun save(zuper: Zuper) {
        // Log.i(TAG, String.format("history add[%d]", history.size))
        history.add(zuper)
        if (history.lastIndex == MAX_DEPTH) {
            // Log.i(TAG, "history add: trimmed last")
            history.removeAt(0)
        }
    }
    fun restore() : Zuper? { // Return null if history is empty
        // Since we need to return value under top, make sure there are 2
        if (history.lastIndex < 1) {
            // Log.i(TAG, "history restore: empty")
            return null
        }
        // Log.i(TAG, String.format("history restore[%d]", history.lastIndex))
        history.removeAt(history.lastIndex)
        return history[history.lastIndex]
    }
}

 */