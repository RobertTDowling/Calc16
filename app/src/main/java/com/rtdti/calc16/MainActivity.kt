package com.rtdti.calc16

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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rtdti.calc16.ui.theme.Calc16Theme
import kotlinx.coroutines.CoroutineScope
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
    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState)}) { _/*innerPadding*/ ->
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
    val pad by viewModel.padState.collectAsStateWithLifecycle()
    val stackState by viewModel.stackState.collectAsStateWithLifecycle()
    val formatState by viewModel.formatState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val stack = stackState.stack
    val formatter = formatState.numberFormat.formatter()
    StackFormatPrime.superscriptFontSizeInt =
        (MaterialTheme.typography.headlineSmall.fontSize.value * 0.7).toInt()
    if (formatState.decimalPlaces == 666) {
        ShowDemoStack(viewModel)
        return
    }
    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxSize()
    ) {
        for (index in stack.size - 1 downTo 0) {
            val text = formatter.format(stack[index], formatState)
            item {
                ShowStackString(text, index, viewModel)
            }
        }
        val LESS_GLITCHY_UI = true
        if (LESS_GLITCHY_UI) {
            if (stack.isEmpty() && pad.pad.isEmpty()) {
                item {
                    ShowStackString(AnnotatedString("Empty"), -1, viewModel)
                }
            }
            item {
                ShowStackPadString(pad.pad)
            }
        } else {
            /* Better aesthetic UI, but due to asynch DB updates, can glitch */
            if (!pad.pad.isEmpty()) {
                item {
                    ShowStackPadString(pad.pad)
                }
            } else if (stack.isEmpty()) {
                item {
                    ShowStackString(AnnotatedString("Empty"), -1, viewModel)
                }
            }
        }
        coroutineScope.launch {
            lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
        }
    }
}

@Composable
fun ShowDemoStack(viewModel: CalcViewModel) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxSize()
    ) {
        val formatState = CalcViewModel.FormatState(1e-4, 0, NumberFormat.IMPROPER)
        ShowStackString(NumberFormat.IMPROPER.formatter().format(Math.PI, formatState), 0, viewModel)
        ShowStackString(NumberFormat.FLOAT.formatter().format(Math.PI, formatState), 0, viewModel)
        ShowStackString(NumberFormat.FIX.formatter().format(1048576.0, formatState), 0, viewModel)
        ShowStackString(NumberFormat.HEX.formatter().format(1048576.0, formatState), 0, viewModel)
        ShowStackString(NumberFormat.PRIME.formatter().format(536870901.0, formatState), 0, viewModel)
        ShowStackString(NumberFormat.MIXIMPERIAL.formatter().format(1+3/16.0, formatState), 0, viewModel)
        ShowStackPadString("2024.0211")
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
    val formatState by viewModel.formatState.collectAsStateWithLifecycle()
    fun onClick() {
        val oldFormat = formatState.numberFormat
        if (oldFormat == newFormat) { // Toggle
            viewModel.numberFormatSet(NumberFormat.FLOAT)
        } else {
            viewModel.numberFormatSet(newFormat)
        }
    }
    val selected: Boolean = formatState.numberFormat == newFormat
    KeyButton(text, ::onClick, Keytype.MODE, selected, crowded)
}

@Composable
fun KeyPad(viewModel: CalcViewModel, snackbarHostState: SnackbarHostState) {
    val formatState by viewModel.formatState.collectAsStateWithLifecycle()
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
            KeyButton(text = " ", { viewModel.pushConstant(formatState.epsilon) }, Keytype.BINOP)
            KeyButton(text = "→ϵ", { viewModel.pop1op({ e -> viewModel.epsilonSet(e)}) }, Keytype.UNOP)
            KeyButton(text = "→.", { viewModel.pop1op({ d -> viewModel.decimalPlacesSet(d.toInt())}) }, Keytype.UNOP)
            KeyButton(text = " ", { viewModel.pushConstant(formatState.decimalPlaces.toDouble()) }, Keytype.BINOP)
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