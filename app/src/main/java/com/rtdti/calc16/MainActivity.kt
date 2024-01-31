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

data class StackEntry(val value: Double)
class Stack() {
    val MAX_DEPTH = 6
    private var entries = MutableList(MAX_DEPTH) { StackEntry(0.0) }
    private val depth = mutableStateOf(0)
    private var pad = mutableStateOf("")
    fun entry(depth: Int) : StackEntry {
        if (hasDepth(depth)) {
            return entries[depth]
        }
        throw IndexOutOfBoundsException()
    }
    fun padGet() : String { return pad.value }
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
            if (isEmpty()) {
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
     Column {
         if (stack.padIsEmpty()) {
             ShowStackString("")
             StackSpacer()
         }
         for (ei in stack.MAX_DEPTH downTo 0) {
             if (stack.hasDepth(ei+1)) {
                 ShowStackEntry(entry = stack.entry(ei))
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
fun ShowStackEntry(entry: StackEntry) {
    ShowStackString(str = entry.value.toString())
}

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
fun KeyPad(stack: Stack) {
    Column {
        Row {

        }
        Row {
            ButtonItem(R.drawable.undo, { stack.padAppend("f") })
            ButtonItem(R.drawable.blank, { stack.padAppend("d") })
            ButtonItem(R.drawable.epsilon, { stack.padAppend("e") })
            ButtonItem(R.drawable.todp, { stack.padAppend("e") })
            ButtonItem(R.drawable.blank, { stack.padAppend("d") })
            ButtonItem(R.drawable.del, { stack.backspaceOrDrop() })
        }
        Row {
            ButtonItem(R.drawable.prime, { stack.padAppend("d") })
            ButtonItem(R.drawable.miximperial, { stack.padAppend("f") })
            ButtonItem(R.drawable.improper, { stack.padAppend("f") })
            ButtonItem(R.drawable.fix, { stack.padAppend("a") })
            ButtonItem(R.drawable.sci, { stack.padAppend("b") })
            ButtonItem(R.drawable.hex, { stack.padAppend("e") })
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
            ButtonItem(R.drawable.not, { stack.unop({a -> 1.0-a}) })
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