package com.rtdti.calc16

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

data class StackEntry(val value: Double)

interface StackFormatter {
    fun format(value: Double, epsilon: Double, dp: Int): String
}

object StackFormatFloat : StackFormatter {
    override fun format(value: Double, epsilon: Double, dp: Int): String {
        return value.toString()
    }
}

object StackFormatHex : StackFormatter {
    fun asLongWithEpsilon(value: Double, epsilon: Double, format: (Long) -> String) : String {
        val truncated = value.toLong()
        val error = value - truncated
        val eString = if (error.absoluteValue > epsilon*epsilon) { " + ϵ" } else { "" } // FIXME
        val without = format(truncated)
        return String.format("%s%s", without, eString)
    }
    override fun format(value: Double, epsilon: Double, dp: Int): String {
        return asLongWithEpsilon(value, epsilon, { tr -> String.format("0x%x", tr) })
    }
}

data class Frac(val num: Long, val denom: Long, val err: Double)

object StackFormatImproper : StackFormatter {
    fun double2frac (startx: Double, epsilon: Double) : Frac {
		// Taken from float_to_frac.c
		//** find rational approximation to given real number
		//** David Eppstein / UC Irvine / 8 Aug 1993
		//**
		//** With corrections from Arno Formella, May 2008

		var x = startx
		val maxden = 1/epsilon;

		var m00 = 1L
        var m11 = 1L
        var m10 = 0L
        var m01 = 0L

        var loops = 0
        var ai = x.toLong()

	    // loop finding terms until denom gets too big
		while (m10 * ai + m11 <= maxden && loops < 100) {
			loops++;
			val t1 = m00 * ai + m01;
			m01 = m00;
			m00 = t1;
			val t2 = m10 * ai + m11;
			m11 = m10;
			m10 = t2;
			if (x==ai.toDouble()) {
                break     // AF: division by zero
            }
			x = 1/(x - ai.toDouble())
			if (x>0x7FFFFFFF) {
                break     // AF: representation failure
            }
            ai = x.toLong()
		}
	    /* now remaining x is between 0 and 1/ai */
	    /* approx as either 0 or 1/m where m is max that will fit in maxden */
	    /* first try zero */
		val n1 = m00
        val d1 = m10
		val e1 = startx - m00.toDouble() / m10
	    /* now try other possibility */
		ai = ((maxden - m11) / m10).toLong();
		m00 = m00 * ai + m01;
		m10 = m10 * ai + m11;

		val n2 = m00
        val d2 = m10;
		val e2 = startx - m00.toDouble() / m10

		if (e1.absoluteValue < e2.absoluteValue) {
			// Pick e1
			return Frac(n1, d1, e1)
		} else {
			// Pick e2
			return Frac(n2, d2, e2)
		}
	}

    override fun format(value: Double, epsilon: Double, dp: Int): String {
        val f = double2frac(value, epsilon)
        val eString = if (f.err.absoluteValue > epsilon*epsilon) { " + ϵ" } else { "" } // FIXME
        var n = f.num
        var d = f.denom
        if (d < 0) {
            d = -d
            n = -n
        }
        if (d == 1L) {
            return String.format("%d%s", n, eString)
        }
        return String.format("%d / %d%s", n, d, eString)
    }
}

object StackFormatterMixImperial : StackFormatter {
    fun gcd(a: Long, b: Long) : Long {
        var a=a
        var b=b
        while (a>0 && b>0) {
            if (a < b) {
                a = b.also({ b = a })
            }
            a %= b
        }
        return if (b < 1) 1L else b
    }
    fun lcm(a: Long, b: Long) : Long {
        return a*b/gcd(a,b)
    }
    fun double2frac(x0: Double, epsilon: Double): Frac {
        val x = Math.abs(x0)
        val invEpsilon2 = Math.pow(2.0, Math.ceil(Math.log(1 / epsilon) / Math.log(2.0)))
        var n: Long = (x * invEpsilon2).roundToLong()
        var d: Long = invEpsilon2.roundToLong()
        if (d < 0) d = -d
        if (d < 1) {
            // avoid trouble with numbers we can't convert
            return Frac(x.roundToLong(), 1L, x-x.roundToLong())
        }
        val g: Long = gcd(n, d)
        n /= g
        d /= g
        if (n == 0L) d = g // fix d=1
        return Frac(n, d, x-n.toDouble()/d)
    }

    override fun format(value: Double, epsilon: Double, dp: Int): String {
        val f = double2frac(value, epsilon)
        val eString = if (f.err.absoluteValue > epsilon*epsilon) { " + ϵ" } else { "" } // FIXME
        var n = f.num
        var d = f.denom
        if (d < 0) {
            d = -d
            n = -n
        }
        var sign = ""
        if (n < 0) {
            sign = "-"
            n = -n
        }
        val w = n / d
        n = n % d
        if (n == 0L) {
            d = 1
        }
        if (d == 1L) {
            return String.format("%s%d%s", sign, w, eString)
        }
        if (w == 0L) {
            return String.format("%s%d / %d%s", sign, n, d, eString)
        }
        return String.format("%s%d - %d / %d%s", sign, w, n, d, eString)
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
                pad.value.toDouble()
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
            Column {
                ShowStack(stack)
                KeyPad(stack)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStack() {
    val stack = Stack()
    stack.push(Math.PI)
    stack.push(2.7182)
    stack.push(1.414)
    ShowStack(stack)
}

@Composable
fun StackSpacer() {
    Spacer(
        modifier = Modifier
            .height(3.dp)
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = RectangleShape
            )
    )
}

@Composable
fun ShowStack(stack: Stack) {
    val formatter = stack.formatter()
     Column {
         if (stack.padIsEmpty()) {
             ShowStackString("")
             StackSpacer()
         }
         for (ei in stack.MAX_DEPTH downTo 0) {
             if (stack.hasDepth(ei+1)) {
                 ShowStackString(formatter.format(stack.entry(ei).value, stack.epsilonGet(), stack.dpGet()))
             } else if (ei==0 && stack.padIsEmpty()) {
                 ShowStackString("Stack Empty")
             } else {
                 ShowStackString("")
             }
             if (ei > 0) {
                 StackSpacer()
             }
         }
         if (!stack.padIsEmpty()) {
             StackSpacer()
             ShowStackPad(stack.padGet())
         }
     }
}

val stackEntryModifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)

@Composable
fun ShowStackString(str: String) {
    Text(text=str,
        color=MaterialTheme.colorScheme.primary,
        style=MaterialTheme.typography.headlineSmall,
        modifier = stackEntryModifier)
}

@Composable
fun ShowStackPad(str: String) {
    Text(text=str,
        color=MaterialTheme.colorScheme.error,
        style=MaterialTheme.typography.headlineSmall,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Bold,
        modifier = stackEntryModifier)
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

        }
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
            ButtonItem(R.drawable.gcd, { stack.padAppend("a") })
            ButtonItem(R.drawable.lcm, { stack.padAppend("b") })
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