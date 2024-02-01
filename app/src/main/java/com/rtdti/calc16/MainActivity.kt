package com.rtdti.calc16

// Todo: Undo
// Todo: Save/Restore state
// Todo: Primes
// Todo: Fix and Sci Modes
// Todo: Animate push and pops (change in stack depth)
// Todo: Pick
// Todo: Mode buttons highlighted
// Todo: Scrollable Stack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

data class StackEntry(val value: Double)

interface StackFormatter {
    fun format(value: Double, epsilon: Double, dp: Int): String
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
    override fun format(value: Double, epsilon: Double, dp: Int): String {
        return value.toString()
    }
}

object StackFormatHex : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int): String {
        val truncated = value.toLong()
        val error = value - truncated
        val eString = makeEString (error, epsilon * epsilon)
        return String.format("0x%x%s", truncated, eString)
    }
}

object StackFormatImproper : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int): String {
        val f = CalcMath.double2frac(value, epsilon)
        return formatFrac(f, epsilon, true)
    }
}

object StackFormatterMixImperial : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int): String {
        val f = CalcMath.double2imperial(value, epsilon)
        return formatFrac(f, epsilon, false)
    }
}

enum class StackFormat { FLOAT, HEX, IMPROPER, MIXIMPERIAL;
    fun formatter() : StackFormatter {
        return when (this) {
            FLOAT -> StackFormatFloat
            HEX -> StackFormatHex
            IMPROPER -> StackFormatImproper
            MIXIMPERIAL -> StackFormatterMixImperial
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
            Column (modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally)
            {
                Row (modifier = Modifier.fillMaxHeight().weight(1f, fill = false)) {
                    ShowStack(stack)
                }
                KeyPad(stack)
            }
        }
    }
}

@Composable
fun ShowStack(stack: Stack) {
    val formatter = stack.formatter()
     Column (modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom) {
         Row (modifier = Modifier.weight(1f, fill = false)) { // Trick to make this not steal everything
             Column() {
                 for (index in stack.depthGet()-1 downTo 0) {
                     ShowStackString(formatter.format(stack.entry(index).value, stack.epsilonGet(), stack.dpGet()))
                 }
             }
         }
         if (!stack.padIsEmpty()) {
             ShowStackPadString(stack.padGet())
         } else if (stack.isEmpty()) {
             ShowStackString("Empty")
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
fun ShowStackString(str: String) {
    StackEntrySurface {
        Text(
            text = str,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            modifier = stackEntryModifier
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

@Composable
fun ButtonItem(
    @DrawableRes imageRes: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick,
        //  modifier = Modifier.padding(0.dp)
        modifier = Modifier
            // .width(72.dp).height(48.dp).scale(2f, 2f)
            // .width(66.dp).height(44.dp).scale(11/6f, 11/6f)
            .width(66.dp)
            .height(44.dp)
            .scale(10 / 6f, 10 / 6f)
            .padding(vertical = 2.dp)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "foo",
        )
    }
}

@Composable
fun ModalFormatButtonItem(
    @DrawableRes imageRes: Int,
    newFormat: StackFormat,
    stack: Stack
) {
    fun onClick() {
        val oldFormat = stack.formatGet()
        if (oldFormat == newFormat) { // Toggle
            stack.formatSet(StackFormat.FLOAT)
            // Stop highlighting this button
        } else {
            stack.formatSet(newFormat)
            // Highlight this button
        }
    }
    ButtonItem(imageRes, ::onClick)
}

@Composable
fun KeyPad(stack: Stack) {
    Column {
        Row {
            ButtonItem(R.drawable.undo, { stack.padAppend("f") })
            ButtonItem(R.drawable.blank, { stack.push(stack.epsilonGet()) })
            ButtonItem(R.drawable.epsilon, { stack.pop1op({e -> stack.epsilonSet(e)}) })
            ButtonItem(R.drawable.todp, { stack.pop1op({d -> stack.dpSet(d.toInt())}) })
            ButtonItem(R.drawable.blank, { stack.push(stack.dpGet().toDouble()) })
            ButtonItem(R.drawable.del, { stack.backspaceOrDrop() })
        }
        Row {
            ModalFormatButtonItem(R.drawable.prime,  StackFormat.FLOAT, stack)
            ModalFormatButtonItem(R.drawable.miximperial,  StackFormat.MIXIMPERIAL, stack)
            ModalFormatButtonItem(R.drawable.improper,  StackFormat.IMPROPER, stack)
            ModalFormatButtonItem(R.drawable.fix, StackFormat.FLOAT, stack)
            ModalFormatButtonItem(R.drawable.sci,  StackFormat.FLOAT, stack)
            ModalFormatButtonItem(R.drawable.hex, StackFormat.HEX, stack)
        }
        Row {
            ButtonItem(R.drawable.floor, { stack.unop({a -> Math.floor(a)}) })
            ButtonItem(R.drawable.round, { stack.unop({a -> Math.round(a).toDouble() }) })
            ButtonItem(R.drawable.ceil, { stack.unop({a -> Math.ceil(a)}) })
            ButtonItem(R.drawable.gcd, { stack.binop({a,b -> CalcMath.gcd(a.toLong(),b.toLong()).toDouble()}) })
            ButtonItem(R.drawable.lcm, { stack.binop({a,b -> CalcMath.lcm(a.toLong(),b.toLong()).toDouble()}) })
            ButtonItem(R.drawable.pi, { stack.padEnter(); stack.push(Math.PI) })
        }
        Row {
            ButtonItem(R.drawable.arcsin, { stack.unop({a -> Math.asin(a)}) })
            ButtonItem(R.drawable.arccos, { stack.unop({a -> Math.acos(a)}) })
            ButtonItem(R.drawable.arctan, { stack.unop({a -> Math.atan(a)}) })
            ButtonItem(R.drawable.exp, { stack.unop({a -> Math.exp(a)}) })
            ButtonItem(R.drawable._2tox, { stack.unop({a -> Math.pow(2.0,a)}) })
            ButtonItem(R.drawable.todeg, { stack.unop({a -> 180*a/Math.PI}) })
        }
        Row {
            ButtonItem(R.drawable.sin, { stack.unop({a -> Math.sin(a)}) })
            ButtonItem(R.drawable.cos, { stack.unop({a -> Math.cos(a)}) })
            ButtonItem(R.drawable.tan, { stack.unop({a -> Math.tan(a)}) })
            ButtonItem(R.drawable.ln, { stack.unop({a -> Math.log(a)}) })
            ButtonItem(R.drawable.log2, { stack.unop({a -> Math.log(a)/Math.log(2.0)}) })
            ButtonItem(R.drawable.degto, { stack.unop({a -> Math.PI*a/180}) })
        }
        Row {
            ButtonItem(R.drawable.d, { stack.padAppend("d") })
            ButtonItem(R.drawable.e, { stack.padAppend("e") })
            ButtonItem(R.drawable.f, { stack.padAppend("f") })
            ButtonItem(R.drawable.not, { stack.unop({a -> -1.0-a}) })
            ButtonItem(R.drawable.times2, { stack.unop({a -> a*2.0}) })
            ButtonItem(R.drawable.divide2, { stack.unop({a -> a/2.0}) })
        }
        Row {
            ButtonItem(R.drawable.a, { stack.padAppend("a") })
            ButtonItem(R.drawable.b, { stack.padAppend("b") })
            ButtonItem(R.drawable.c, { stack.padAppend("c") })
            ButtonItem(R.drawable.and, { stack.binop({a,b -> a.toLong().and(b.toLong()).toDouble()}) })
            ButtonItem(R.drawable.or, { stack.binop({a,b -> a.toLong().or(b.toLong()).toDouble()}) })
            ButtonItem(R.drawable.xor, { stack.binop({a,b -> a.toLong().xor(b.toLong()).toDouble()}) })
        }
        Row {
            ButtonItem(R.drawable._7, { stack.padAppend("7")})
            ButtonItem(R.drawable._8, { stack.padAppend("8")})
            ButtonItem(R.drawable._9, { stack.padAppend("9")})
            ButtonItem(R.drawable.divide, { stack.binop({a,b -> a/b}) })
            ButtonItem(R.drawable.invx, { stack.unop({a -> 1.0/a}) })
            ButtonItem(R.drawable.mod, { stack.binop({a,b -> a%b}) })
        }
        Row {
            ButtonItem(R.drawable._4, { stack.padAppend("4")})
            ButtonItem(R.drawable._5, { stack.padAppend("5")})
            ButtonItem(R.drawable._6, { stack.padAppend("6")})
            ButtonItem(R.drawable.times, { stack.binop({a,b -> a*b}) })
            ButtonItem(R.drawable.ytox, { stack.binop({a,b -> Math.pow(a,b)}) })
            ButtonItem(R.drawable.ee, { stack.binop({a,b -> a*Math.pow(10.0,b)})})
        }
        Row {
            ButtonItem(R.drawable._1, { stack.padAppend("1")})
            ButtonItem(R.drawable._2, { stack.padAppend("2")})
            ButtonItem(R.drawable._3, { stack.padAppend("3")})
            ButtonItem(R.drawable.minus, { stack.binop({a,b -> a-b}) })
            ButtonItem(R.drawable.plusminus, {  stack.unop({a -> -a}) })
            ButtonItem(R.drawable.blank, {})
        }
        Row {
            ButtonItem(R.drawable.enter, { stack.enterOrDup() })
            ButtonItem(R.drawable._0, { stack.padAppend("0")})
            ButtonItem(R.drawable.point, { stack.padAppend(".")})
            ButtonItem(R.drawable.plus, { stack.binop({a,b -> a+b}) })
            ButtonItem(R.drawable.root, { stack.unop({a -> Math.sqrt(a)}) })
            ButtonItem(R.drawable.swap, { stack.swap() })
        }
    }
}