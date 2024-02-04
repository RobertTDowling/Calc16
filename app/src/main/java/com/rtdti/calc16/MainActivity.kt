package com.rtdti.calc16

// Todo: Undo
// Todo: Save/Restore state
// Todo: Animate push and pops (change in stack depth)
// Todo: Scrollable Stack with automatic reveal at bottom on any action

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rtdti.calc16.ui.theme.Calc16Theme
import kotlin.math.absoluteValue

data class StackEntry(val value: Double)

interface StackFormatter {
    fun format(value: Double, epsilon: Double, dp: Int, fs: Int): AnnotatedString
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
    override fun format(value: Double, epsilon: Double, dp: Int, fs: Int): AnnotatedString {
        return AnnotatedString(value.toString())
    }
}

object StackFormatHex : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int, fs: Int): AnnotatedString {
        val truncated = value.toLong()
        val error = value - truncated
        val eString = makeEString (error, epsilon * epsilon)
        return AnnotatedString(String.format("0x%x%s", truncated, eString))
    }
}

object StackFormatImproper : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int, fs: Int): AnnotatedString {
        val f = CalcMath.double2frac(value, epsilon)
        return AnnotatedString(formatFrac(f, epsilon, true))
    }
}

object StackFormatMixImperial : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int, fs: Int): AnnotatedString {
        val f = CalcMath.double2imperial(value, epsilon)
        return AnnotatedString(formatFrac(f, epsilon, false))
    }
}

object StackFormatFix : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int, fs: Int): AnnotatedString {
        return AnnotatedString(String.format(String.format("%%.%df", dp), value))
    }
}

object StackFormatSci : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int, fs: Int): AnnotatedString {
        return AnnotatedString(String.format(String.format("%%.%de", dp), value))
    }
}

object StackFormatPrime : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int, fs: Int): AnnotatedString {
        return CalcMath.primeFactorAnnotatedString(value.toLong(), fs)
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
    val MAX_DEPTH = 6
    private val entries = MutableList(MAX_DEPTH) { StackEntry(0.0) }
    private val depth = mutableStateOf(0)
    private val pad = mutableStateOf("")
    private val format = mutableStateOf(StackFormat.FLOAT)
    private val epsilon = mutableStateOf(1e-4)
    private val dp = mutableStateOf(2)
    fun dpGet(): Int { return dp.value }
    fun dpSet(d: Int) { dp.value = d }
    fun epsilonGet(): Double { return epsilon.value }
    fun epsilonSet(e: Double) { epsilon.value = e }
    fun entry(depth: Int) : StackEntry {
        if (hasDepth(depth)) {
            return entries[depth]
        }
        throw IndexOutOfBoundsException()
    }
    fun depthGet() : Int { return depth.value }
    fun padGet() : String { return pad.value }
    fun formatGet() : StackFormat { return format.value }
    fun formatSet(fmt: StackFormat) { format.value = fmt }
    fun formatter(): StackFormatter { return format.value.formatter() }
    fun push(x: Double) {
        entries.add(0, StackEntry(x))
        depth.value = depth.value + 1
    }
    fun pop(): Double {
        depth.value = depth.value - 1
        return entries.removeAt(0).value
    }
    fun pick(index: Int) {
        push(entries[index].value)
    }
    fun swap() {
        padEnter()
        if (hasDepth(2)) {
            val b = pop()
            val a = pop()
            push(b)
            push(a)
        }
    }
    fun binop(op: (Double, Double) -> Double) {
        padEnter()
        if (hasDepth(2)) {
            val b = pop()
            val a = pop()
            push(op(a, b))
        }
    }
    fun unop(op: (Double) -> Double) {
        padEnter()
        if (hasDepth(1)) {
            val a = pop()
            push(op(a))
        }
    }
    fun pop1op(op: (Double) -> Unit) {
        padEnter()
        if (hasDepth(1)) {
            val a = pop()
            op(a)
        }
    }

    fun hasDepth(d: Int): Boolean {
        return depth.value >= d
    }
    fun isEmpty(): Boolean { return !hasDepth(1)}

    fun enterOrDup() { // Combo enter and dup
        if (!padIsEmpty()) {
            padEnter()
        } else {
            if (!isEmpty()) {
                val a = pop();
                push(a)
                push(a)
            }
        }
    }
    fun backspaceOrDrop() { // Combo backspace and drop
        if (padIsEmpty()) {
            if (!isEmpty()) {
                pop()
            }
        } else {
            padDelete()
        }
    }

    fun padAppend(str: String) {
        pad.value = pad.value + str
    }
    fun padClear() {
        pad.value = ""
    }
    fun padDelete() {
        if (!padIsEmpty()) {
            pad.value = pad.value.substring(0, pad.value.length-1)
        }
    }
    fun padIsEmpty(): Boolean {
        return pad.value.length == 0
    }
    fun padEnter() {
        if (padIsEmpty()) {
            // Do nothing?
        } else {
            val x = try {
                if (format.value == StackFormat.HEX) {
                    pad.value.toLong(radix = 16).toDouble()
                } else {
                    pad.value.toDouble()
                }
            } catch (e: Exception) {
                0.0
            }
            push(x)
            padClear()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stack = Stack()
        setContent {
            Calc16Theme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background)
                {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    )
                    {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f, fill = false)
                        ) {
                            ShowStack(stack)
                        }
                        KeyPad(stack)
                    }
                }
            }
        }
    }
}

@Composable
fun ShowStack(stack: Stack) {
    val formatter = stack.formatter()
     Column (modifier = Modifier
         .fillMaxSize()
         .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom) {
         Row (modifier = Modifier.weight(1f, fill = false)) { // Trick to make this not steal everything
             Column() {
                 for (index in stack.depthGet()-1 downTo 0) {
                     ShowStackString(
                         formatter.format(stack.entry(index).value, stack.epsilonGet(), stack.dpGet(),
                             (MaterialTheme.typography.headlineSmall.fontSize.value * 0.7).toInt()),
                         index, stack)
                 }
             }
         }
         if (!stack.padIsEmpty()) {
             ShowStackPadString(stack.padGet())
         } else if (stack.isEmpty()) {
             ShowStackString(AnnotatedString("Empty"), -1, stack)
         }

     }
}

val stackEntryModifier = Modifier.padding(vertical = 0.dp, horizontal = 8.dp)
val stackSurfaceModifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)

@Composable
fun StackEntrySurface(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = stackSurfaceModifier.fillMaxWidth(),
        content = content)
}

@Composable
fun ShowStackString(str: AnnotatedString, index: Int, stack: Stack) {
    StackEntrySurface {
        Text(
            text = str,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            modifier = stackEntryModifier
                .clickable { if (index >= 0) stack.pick(index) }
        )
    }
}

@Composable
fun ShowStackPadString(str: String) {
    StackEntrySurface {
        Text(text=str,
            color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.headlineSmall,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            modifier = stackEntryModifier)
    }
}

val rowModifier = Modifier.fillMaxWidth()
val rowArragement = Arrangement.SpaceBetween
val colModifier = Modifier
    .fillMaxWidth()
    .padding(vertical = 2.dp, horizontal = 8.dp)
val keySurfaceModifier = Modifier.padding(2.dp)
val keyTextModifier = Modifier
    .clickable(onClick = {})
    //.width(44.dp).height(24.dp)
    .width(60.dp).height(40.dp)
    .padding(horizontal = 8.dp, vertical = 0.dp) // 8

enum class Keytype { CONTROL, ENTRY, UNOP, BINOP, TRIG, MODE }
@Composable
fun fgColor(typex: Keytype): Color {
    return when (typex) {
        Keytype.CONTROL -> MaterialTheme.colorScheme.inverseSurface
        Keytype.ENTRY -> MaterialTheme.colorScheme.primary
        Keytype.UNOP -> MaterialTheme.colorScheme.secondary
        Keytype.BINOP -> MaterialTheme.colorScheme.tertiary
        Keytype.TRIG -> MaterialTheme.colorScheme.tertiary
        Keytype.MODE -> MaterialTheme.colorScheme.errorContainer
    }
}
@Composable
fun bgColor(typex: Keytype): Color {
    return when (typex) {
        Keytype.CONTROL -> MaterialTheme.colorScheme.background
        Keytype.ENTRY -> MaterialTheme.colorScheme.onPrimary
        Keytype.UNOP -> MaterialTheme.colorScheme.onSecondary
        Keytype.BINOP -> MaterialTheme.colorScheme.onTertiary
        Keytype.TRIG -> MaterialTheme.colorScheme.onSecondary
        Keytype.MODE -> MaterialTheme.colorScheme.onErrorContainer
    }
}

@Composable
fun btop(b: String, p: String, raised: Boolean = false): AnnotatedString {
    return buildAnnotatedString {
        if (raised) {
            // pushStyle(style = SpanStyle(baselineShift = BaselineShift(-0.7f)))
        }
        append(b)
        withStyle(
            style = SpanStyle(
                baselineShift = BaselineShift.Superscript,
                fontSize = MaterialTheme.typography.bodySmall.fontSize
            )
        ) { append(p) }
    }
}

@Composable
fun KeyButton(text: String, onClick: () -> Unit, type: Keytype, selected: Boolean = false, crowded: Boolean = false) {
    KeyButton(text = AnnotatedString(text), onClick, type, selected, crowded)
}

@Composable
fun KeyButton(text: AnnotatedString, onClick: () -> Unit, type: Keytype, selected: Boolean = false, crowded: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.error else bgColor(type),
        modifier = keySurfaceModifier
    ) {
        val mod = if (crowded) keyTextModifier.padding(top = 8.dp) else keyTextModifier.padding(top = 4.dp)
        Text(
            text = text,
            color = fgColor(type),
            modifier = mod.clickable { onClick() },
            fontSize = if (crowded)
                MaterialTheme.typography.titleMedium.fontSize
            else
                MaterialTheme.typography.titleLarge.fontSize,
        )
    }
}
@Composable
fun ModalKeyButton(text: String, newFormat: StackFormat, stack: Stack, crowded: Boolean = false) {
    fun onClick() {
        val oldFormat = stack.formatGet()
        if (oldFormat == newFormat) { // Toggle
            stack.formatSet(StackFormat.FLOAT)
        } else {
            stack.formatSet(newFormat)
        }
    }
    val selected: Boolean = stack.formatGet() == newFormat
    KeyButton(text, ::onClick, Keytype.MODE, selected, crowded)
}

@Composable
fun KeyPad(stack: Stack) {
    Column (
        modifier = colModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "⤺", {  }, Keytype.CONTROL)
            KeyButton(text = " ", { stack.push(stack.epsilonGet()) }, Keytype.BINOP)
            KeyButton(text = "→ϵ", { stack.pop1op({ e -> stack.epsilonSet(e)}) }, Keytype.UNOP)
            KeyButton(text = "→.", { stack.pop1op({ d -> stack.dpSet(d.toInt())}) }, Keytype.UNOP)
            KeyButton(text = " ", { stack.push(stack.dpGet().toDouble()) }, Keytype.BINOP)
            KeyButton(text = "◀", { stack.backspaceOrDrop() }, Keytype.CONTROL)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            ModalKeyButton(text = "2³·5⁷", StackFormat.PRIME, stack, crowded = true)
            ModalKeyButton(text = "1-¾", StackFormat.MIXIMPERIAL, stack)
            ModalKeyButton(text = "⅖", StackFormat.IMPROPER, stack)
            ModalKeyButton(text = "[1.23]", StackFormat.FIX, stack, crowded = true)
            ModalKeyButton(text = "1e+0", StackFormat.SCI, stack, crowded = true)
            ModalKeyButton(text = "x₁₆", StackFormat.HEX, stack)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "⎣x⎦", { stack.unop({ a -> Math.floor(a)}) }, Keytype.UNOP)
            KeyButton(text = "[x]", { stack.unop({ a -> Math.round(a).toDouble() }) }, Keytype.UNOP)
            KeyButton(text = "⎡x⎤", { stack.unop({ a -> Math.ceil(a)}) }, Keytype.UNOP)
            KeyButton(text = "gcd", { stack.binop({ a, b -> CalcMath.gcd(a.toLong(),b.toLong()).toDouble()}) }, Keytype.BINOP)
            KeyButton(text = "lcm", { stack.binop({ a, b -> CalcMath.lcm(a.toLong(),b.toLong()).toDouble()}) }, Keytype.BINOP)
            KeyButton(text = " ", { stack.push(stack.depthGet().toDouble()) }, Keytype.BINOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = btop("sin","-1"), { stack.unop({ a -> Math.asin(a)}) }, Keytype.TRIG, crowded = true)
            KeyButton(text = btop("cos","-1"), { stack.unop({ a -> Math.acos(a)}) }, Keytype.TRIG, crowded = true)
            KeyButton(text = btop("tan","-1"), { stack.unop({ a -> Math.atan(a)}) }, Keytype.TRIG, crowded = true)
            KeyButton(text = btop("e","x"), { stack.unop({ a -> Math.exp(a)}) }, Keytype.TRIG)
            KeyButton(text = btop("2","x"), { stack.unop({ a -> Math.pow(2.0,a)}) }, Keytype.TRIG)
            KeyButton(text = "r→⚬", { stack.unop({ a -> 180*a/Math.PI}) }, Keytype.TRIG)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "sin", { stack.unop({ a -> Math.sin(a)}) }, Keytype.TRIG)
            KeyButton(text = "cos", { stack.unop({ a -> Math.cos(a)}) }, Keytype.TRIG)
            KeyButton(text = "tan", { stack.unop({ a -> Math.tan(a)}) }, Keytype.TRIG)
            KeyButton(text = "log", { stack.unop({ a -> Math.log(a)}) }, Keytype.TRIG)
            KeyButton(text = "log₂", { stack.unop({ a -> Math.log(a)/Math.log(2.0)}) }, Keytype.TRIG)
            KeyButton(text = "⚬→r", { stack.unop({ a -> Math.PI*a/180}) }, Keytype.TRIG)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "D", { stack.padAppend("d") }, Keytype.ENTRY)
            KeyButton(text = "E", { stack.padAppend("e") }, Keytype.ENTRY)
            KeyButton(text = "F", { stack.padAppend("f") }, Keytype.ENTRY)
            KeyButton(text = "¬", { stack.unop({ a -> -1.0-a}) }, Keytype.UNOP)
            KeyButton(text = "2×", { stack.unop({ a -> a*2.0}) }, Keytype.UNOP)
            KeyButton(text = "2÷", { stack.unop({ a -> a/2.0}) }, Keytype.UNOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "A", { stack.padAppend("a") }, Keytype.ENTRY)
            KeyButton(text = "B", { stack.padAppend("b") }, Keytype.ENTRY)
            KeyButton(text = "C", { stack.padAppend("c") }, Keytype.ENTRY)
            KeyButton(text = "∧", { stack.binop({ a, b -> a.toLong().and(b.toLong()).toDouble()}) }, Keytype.BINOP)
            KeyButton(text = "∨", { stack.binop({ a, b -> a.toLong().or(b.toLong()).toDouble()}) }, Keytype.BINOP)
            KeyButton(text = "⨁", { stack.binop({ a, b -> a.toLong().xor(b.toLong()).toDouble()}) }, Keytype.BINOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "7", { stack.padAppend("7") }, Keytype.ENTRY)
            KeyButton(text = "8", { stack.padAppend("8") }, Keytype.ENTRY)
            KeyButton(text = "9", { stack.padAppend("9") }, Keytype.ENTRY)
            KeyButton(text = "÷", { stack.binop({ a, b -> a/b}) }, Keytype.BINOP)
            KeyButton(text = "1/x", { stack.unop({ a -> 1.0/a}) }, Keytype.UNOP)
            KeyButton(text = "mod", { stack.binop({ a, b -> a%b}) }, Keytype.BINOP, crowded = true)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "4", { stack.padAppend("4") }, Keytype.ENTRY)
            KeyButton(text = "5", { stack.padAppend("5") }, Keytype.ENTRY)
            KeyButton(text = "6", { stack.padAppend("6") }, Keytype.ENTRY)
            KeyButton(text = "×", { stack.binop({ a, b -> a*b}) }, Keytype.BINOP)
            KeyButton(text = btop("y","x",true), { stack.binop({ a, b -> Math.pow(a,b)}) }, Keytype.BINOP)
            KeyButton(text = btop("y10","x"), { stack.binop({ a, b -> a*Math.pow(10.0,b)}) }, Keytype.BINOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "1", { stack.padAppend("1") }, Keytype.ENTRY)
            KeyButton(text = "2", { stack.padAppend("2") }, Keytype.ENTRY)
            KeyButton(text = "3", { stack.padAppend("3") }, Keytype.ENTRY)
            KeyButton(text = "−", { stack.binop({ a, b -> a-b}) }, Keytype.BINOP)
            KeyButton(text = "±", { stack.unop({ a -> -a}) }, Keytype.UNOP)
            KeyButton(text = "π", { stack.padEnter(); stack.push(Math.PI) }, Keytype.ENTRY)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "▲", { stack.enterOrDup() }, Keytype.CONTROL)
            KeyButton(text = "0", { stack.padAppend("0") }, Keytype.ENTRY)
            KeyButton(text = ".", { stack.padAppend(".") }, Keytype.ENTRY)
            KeyButton(text = "+", { stack.binop({ a, b -> a+b}) }, Keytype.BINOP)
            KeyButton(text = "√x", { stack.unop({ a -> Math.sqrt(a)}) }, Keytype.UNOP)
            KeyButton(text = "x⇄y", { stack.swap() }, Keytype.CONTROL, crowded = true)
        }
    }
}