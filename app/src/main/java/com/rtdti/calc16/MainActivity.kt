package com.rtdti.calc16

// Todo: Save/Restore state
// Todo: Animate push and pops (change in stack depth)

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rtdti.calc16.ui.theme.Calc16Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: CalcViewModel = viewModel(factory = AppViewModelProvider.Factory)
            TheScaffold(viewModel)
        }
    }
}

@Composable
fun TheScaffold(viewModel: CalcViewModel) { // Needed to show snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState)}) { innerPadding ->
        Calc16Theme {
            Surface(modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background)
            {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                {
                    Row(modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f, fill = false)
                    ) {
                        ShowStack(viewModel)
                    }
                    KeyPad(viewModel, snackbarHostState)
                    ShowDebug(viewModel)
                }
            }
        }
    }
}

@Composable
fun ShowDebug(viewModel: CalcViewModel) {
    Text(viewModel.debugString.value)
}

@Composable
fun ShowStack(viewModel: CalcViewModel) {
    val pad by viewModel.padState.collectAsState()
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val stackState by viewModel.stackState.collectAsState()
    val stack = stackState.stack
    val formatter = viewModel.formatter()
    viewModel.formatParameters.superscriptFontSizeInt.value = (MaterialTheme.typography.headlineSmall.fontSize.value * 0.7).toInt()
    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .fillMaxSize()
    ) {
        for (index in stack.size-1 downTo 0) {
            val text = AnnotatedString(String.format("%d: ", index)) + formatter.format(stack[index], viewModel.formatParameters)
            item {
                ShowStackString(text, index, viewModel)
            }
        }
        if (!pad.isEmpty()) {
            item {
                ShowStackPadString(pad.pad)
            }
        } else if (stack.isEmpty()) {
            item {
                ShowStackString(AnnotatedString("Empty"), -1, viewModel)
            }
        }
        coroutineScope.launch {
            lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
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
fun ShowStackString(str: AnnotatedString, index: Int, viewModel: CalcViewModel) {
    StackEntrySurface {
        Text(
            text = str,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            modifier = stackEntryModifier
                .clickable { if (index >= 0) viewModel.pick(index) }
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
    .width(60.dp)
    .height(40.dp)
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
fun ModalKeyButton(text: String, newFormat: NumberFormat, viewModel: CalcViewModel, crowded: Boolean = false) {
    fun onClick() {
        val oldFormat = viewModel.formatGet()
        if (oldFormat == newFormat) { // Toggle
            viewModel.formatSet(NumberFormat.FLOAT)
        } else {
            viewModel.formatSet(newFormat)
        }
    }
    val selected: Boolean = viewModel.formatGet() == newFormat
    KeyButton(text, ::onClick, Keytype.MODE, selected, crowded)
}

@Composable
fun KeyPad(viewModel: CalcViewModel, snackbarHostState: SnackbarHostState) {
    val coroutineScope = rememberCoroutineScope()
    Column (
        modifier = colModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            fun noMoreUndos() { coroutineScope.launch { snackbarHostState.showSnackbar("No More Undos")} }
            KeyButton(text = "⤺", { if (viewModel.stackRollBack()) noMoreUndos() }, Keytype.CONTROL)
            KeyButton(text = " ", { viewModel.pushConstant(viewModel.formatParameters.epsilon.value) }, Keytype.BINOP)
            KeyButton(text = "→ϵ", { viewModel.pop1op({ e -> viewModel.formatParameters.epsilon.value = e}) }, Keytype.UNOP)
            KeyButton(text = "→.", { viewModel.pop1op({ d -> viewModel.formatParameters.decimalPlaces.value = d.toInt()}) }, Keytype.UNOP)
            KeyButton(text = " ", { viewModel.pushConstant(viewModel.formatParameters.decimalPlaces.value.toDouble()) }, Keytype.BINOP)
            KeyButton(text = "◀", { viewModel.backspaceOrDrop() }, Keytype.CONTROL)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            ModalKeyButton(text = "2³·5⁷", NumberFormat.PRIME, viewModel, crowded = true)
            ModalKeyButton(text = "1-¾", NumberFormat.MIXIMPERIAL, viewModel)
            ModalKeyButton(text = "⅖", NumberFormat.IMPROPER, viewModel)
            ModalKeyButton(text = "[1.23]", NumberFormat.FIX, viewModel, crowded = true)
            ModalKeyButton(text = "1e+0", NumberFormat.SCI, viewModel, crowded = true)
            ModalKeyButton(text = "x₁₆", NumberFormat.HEX, viewModel)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "⎣x⎦", { viewModel.unop({ a -> Math.floor(a)}) }, Keytype.UNOP)
            KeyButton(text = "[x]", { viewModel.unop({ a -> Math.round(a).toDouble() }) }, Keytype.UNOP)
            KeyButton(text = "⎡x⎤", { viewModel.unop({ a -> Math.ceil(a)}) }, Keytype.UNOP)
            KeyButton(text = "gcd", { viewModel.binop({ a, b -> CalcMath.gcd(a.toLong(),b.toLong()).toDouble()}) }, Keytype.BINOP)
            KeyButton(text = "lcm", { viewModel.binop({ a, b -> CalcMath.lcm(a.toLong(),b.toLong()).toDouble()}) }, Keytype.BINOP)
            KeyButton(text = " ", { viewModel.pushConstant(viewModel.stackDepth().toDouble()) }, Keytype.BINOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = btop("sin","-1"), { viewModel.unop({ a -> Math.asin(a)}) }, Keytype.TRIG, crowded = true)
            KeyButton(text = btop("cos","-1"), { viewModel.unop({ a -> Math.acos(a)}) }, Keytype.TRIG, crowded = true)
            KeyButton(text = btop("tan","-1"), { viewModel.unop({ a -> Math.atan(a)}) }, Keytype.TRIG, crowded = true)
            KeyButton(text = btop("e","x"), { viewModel.unop({ a -> Math.exp(a)}) }, Keytype.TRIG)
            KeyButton(text = btop("2","x"), { viewModel.unop({ a -> Math.pow(2.0,a)}) }, Keytype.TRIG)
            KeyButton(text = "r→⚬", { viewModel.unop({ a -> 180*a/Math.PI}) }, Keytype.TRIG)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "sin", { viewModel.unop({ a -> Math.sin(a)}) }, Keytype.TRIG)
            KeyButton(text = "cos", { viewModel.unop({ a -> Math.cos(a)}) }, Keytype.TRIG)
            KeyButton(text = "tan", { viewModel.unop({ a -> Math.tan(a)}) }, Keytype.TRIG)
            KeyButton(text = "log", { viewModel.unop({ a -> Math.log(a)}) }, Keytype.TRIG)
            KeyButton(text = "log₂", { viewModel.unop({ a -> Math.log(a)/Math.log(2.0)}) }, Keytype.TRIG)
            KeyButton(text = "⚬→r", { viewModel.unop({ a -> Math.PI*a/180}) }, Keytype.TRIG)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "D", { coroutineScope.launch { viewModel.padAppend("d") }}, Keytype.ENTRY)
            KeyButton(text = "E", { viewModel.padAppend("e") }, Keytype.ENTRY)
            KeyButton(text = "F", { viewModel.padAppend("f") }, Keytype.ENTRY)
            KeyButton(text = "¬", { viewModel.unop({ a -> -1.0-a}) }, Keytype.UNOP)
            KeyButton(text = "2×", { viewModel.unop({ a -> a*2.0}) }, Keytype.UNOP)
            KeyButton(text = "2÷", { viewModel.unop({ a -> a/2.0}) }, Keytype.UNOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "A", { viewModel.padAppend("a") }, Keytype.ENTRY)
            KeyButton(text = "B", { viewModel.padAppend("b") }, Keytype.ENTRY)
            KeyButton(text = "C", { viewModel.padAppend("c") }, Keytype.ENTRY)
            KeyButton(text = "∧", { viewModel.binop({ a, b -> a.toLong().and(b.toLong()).toDouble()}) }, Keytype.BINOP)
            KeyButton(text = "∨", { viewModel.binop({ a, b -> a.toLong().or(b.toLong()).toDouble()}) }, Keytype.BINOP)
            KeyButton(text = "⨁", { viewModel.binop({ a, b -> a.toLong().xor(b.toLong()).toDouble()}) }, Keytype.BINOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "7", { viewModel.padAppend("7") }, Keytype.ENTRY)
            KeyButton(text = "8", { viewModel.padAppend("8") }, Keytype.ENTRY)
            KeyButton(text = "9", { viewModel.padAppend("9") }, Keytype.ENTRY)
            KeyButton(text = "÷", { viewModel.binop({ a, b -> a/b}) }, Keytype.BINOP)
            KeyButton(text = "1/x", { viewModel.unop({ a -> 1.0/a}) }, Keytype.UNOP)
            KeyButton(text = "mod", { viewModel.binop({ a, b -> a%b}) }, Keytype.BINOP, crowded = true)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "4", { viewModel.padAppend("4") }, Keytype.ENTRY)
            KeyButton(text = "5", { viewModel.padAppend("5") }, Keytype.ENTRY)
            KeyButton(text = "6", { viewModel.padAppend("6") }, Keytype.ENTRY)
            KeyButton(text = "×", { viewModel.binop({ a, b -> a*b}) }, Keytype.BINOP)
            KeyButton(text = btop("y","x",true), { viewModel.binop({ a, b -> Math.pow(a,b)}) }, Keytype.BINOP)
            KeyButton(text = btop("y10","x"), { viewModel.binop({ a, b -> a*Math.pow(10.0,b)}) }, Keytype.BINOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "1", { viewModel.padAppend("1") }, Keytype.ENTRY)
            KeyButton(text = "2", { viewModel.padAppend("2") }, Keytype.ENTRY)
            KeyButton(text = "3", { viewModel.padAppend("3") }, Keytype.ENTRY)
            KeyButton(text = "−", { viewModel.binop({ a, b -> a-b}) }, Keytype.BINOP)
            KeyButton(text = "±", { viewModel.unop({ a -> -a}) }, Keytype.UNOP)
            KeyButton(text = "π", { viewModel.pushConstant(Math.PI) }, Keytype.ENTRY)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "▲", { viewModel.enterOrDup() }, Keytype.CONTROL)
            KeyButton(text = "0", { viewModel.padAppend("0") }, Keytype.ENTRY)
            KeyButton(text = ".", { viewModel.padAppend(".") }, Keytype.ENTRY)
            KeyButton(text = "+", { viewModel.binop({ a, b -> a+b}) }, Keytype.BINOP)
            KeyButton(text = "√x", { viewModel.unop({ a -> Math.sqrt(a)}) }, Keytype.UNOP)
            KeyButton(text = "x⇄y", { viewModel.swap() }, Keytype.CONTROL, crowded = true)
        }
    }
}