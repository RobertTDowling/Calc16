package com.rtdti.calc16

import android.os.Bundle
import android.util.Log
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

data class StackEntry(val entry: Double)
class Stack() {
    val MAX_DEPTH = 10
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
        return entries.removeAt(0).entry
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
    ShowStackString(str = entry.entry.toString())
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
            ButtonItem(R.drawable._7, { stack.padAppend("7")})
            ButtonItem(R.drawable._8, { stack.padAppend("8")})
            ButtonItem(R.drawable._9, { stack.padAppend("9")})
            ButtonItem(R.drawable.divide, { stack.binop({a,b -> a/b}) })
            ButtonItem(R.drawable.invx, { stack.unop({a -> 1.0/a}) })
            ButtonItem(R.drawable.del, {
                if (stack.padIsEmpty()) {
                    if (!stack.isEmpty()) {
                        stack.pop()
                    }
                } else {
                    stack.padDelete()
                }})
        }
        Row {
            ButtonItem(R.drawable._4, { stack.padAppend("4")})
            ButtonItem(R.drawable._5, { stack.padAppend("5")})
            ButtonItem(R.drawable._6, { stack.padAppend("6")})
            ButtonItem(R.drawable.times, { stack.binop({a,b -> a*b}) })
            ButtonItem(R.drawable.ytox, { stack.binop({a,b -> Math.pow(a,b)}) })
            ButtonItem(R.drawable.todp, {})
        }
        Row {
            ButtonItem(R.drawable._1, { stack.padAppend("1")})
            ButtonItem(R.drawable._2, { stack.padAppend("2")})
            ButtonItem(R.drawable._3, { stack.padAppend("3")})
            ButtonItem(R.drawable.minus, { stack.binop({a,b -> a-b}) })
            ButtonItem(R.drawable.plusminus, {  stack.unop({a -> -a}) })
        }
        Row {
            ButtonItem(R.drawable._0, { stack.padAppend("0")})
            ButtonItem(R.drawable.point, { stack.padAppend(".")})
            ButtonItem(R.drawable.enter, {
                if (!stack.padIsEmpty()) {
                    stack.padEnter()
                } else {
                    if (!stack.isEmpty()) {
                        val a = stack.pop();
                        stack.push(a)
                        stack.push(a)
                    }}})
            ButtonItem(R.drawable.plus, { stack.binop({a,b -> a+b}) })
            ButtonItem(R.drawable.swap, {
                stack.padEnter()
                if (stack.hasDepth(2)) {
                    val b = stack.pop()
                    val a = stack.pop()
                    stack.push(b)
                    stack.push(a)
                }})
        }
    }
}