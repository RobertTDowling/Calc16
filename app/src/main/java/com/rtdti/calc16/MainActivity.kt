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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val everythingState by viewModel.everythingState.collectAsStateWithLifecycle()
    val pad = everythingState.padState.pad
    val stackState = everythingState.stackState
    val formatState = everythingState.formatState
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val stack = stackState.stack
    val formatter = Formatter(formatState, (MaterialTheme.typography.headlineSmall.fontSize.value * 0.7).toInt())
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
            val text = formatter.format(stack[index])
            item {
                ShowStackString(text, index, viewModel)
            }
        }
        val LESS_GLITCHY_UI = false
        if (LESS_GLITCHY_UI) {
            if (stack.isEmpty() && pad.isEmpty()) {
                item {
                    ShowStackString(AnnotatedString("Empty"), -1, viewModel)
                }
            }
            item {
                ShowStackPadString(pad)
            }
        } else {
            /* Better aesthetic UI, but due to asynch DB updates, can glitch */
            if (!pad.isEmpty()) {
                item {
                    ShowStackPadString(pad)
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
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxSize()
    ) {
        val eps = 1e-4
        val dp = 0
        val ss = (MaterialTheme.typography.headlineSmall.fontSize.value * 0.7).toInt()
        ShowStackString(Formatter(CalcViewModel.FormatState(eps, dp, NumberFormat.IMPROPER), ss).format(Math.PI), 0, viewModel)
        ShowStackString(Formatter(CalcViewModel.FormatState(eps, dp, NumberFormat.FLOAT), ss).format(Math.PI), 0, viewModel)
        ShowStackString(Formatter(CalcViewModel.FormatState(eps, dp, NumberFormat.TIME), ss).format(4.75), 0, viewModel)
        ShowStackString(Formatter(CalcViewModel.FormatState(eps, dp, NumberFormat.HEX), ss).format(1048576.0), 0, viewModel)
        ShowStackString(Formatter(CalcViewModel.FormatState(eps, dp, NumberFormat.PRIME), ss).format(536870901.0), 0, viewModel)
        ShowStackString(Formatter(CalcViewModel.FormatState(eps, dp, NumberFormat.MIXIMPERIAL), ss).format(1+3/16.0), 0, viewModel)
        ShowStackPadString("2024.0218")
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
        Keytype.CONTROL -> MaterialTheme.colorScheme.surface
        Keytype.ENTRY -> MaterialTheme.colorScheme.onPrimaryContainer
        Keytype.UNOP -> MaterialTheme.colorScheme.onSecondaryContainer
        Keytype.BINOP -> MaterialTheme.colorScheme.onTertiaryContainer
        Keytype.TRIG -> MaterialTheme.colorScheme.onTertiaryContainer
        Keytype.MODE -> MaterialTheme.colorScheme.onErrorContainer
    }
}
@Composable
fun bgColor(typex: Keytype): Color {
    return when (typex) {
        Keytype.CONTROL -> MaterialTheme.colorScheme.inverseSurface
        Keytype.ENTRY -> MaterialTheme.colorScheme.primaryContainer
        Keytype.UNOP -> MaterialTheme.colorScheme.secondaryContainer
        Keytype.BINOP -> MaterialTheme.colorScheme.tertiaryContainer
        Keytype.TRIG -> MaterialTheme.colorScheme.secondaryContainer
        Keytype.MODE -> MaterialTheme.colorScheme.errorContainer
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
        color = if (selected) fgColor(type) else bgColor(type),
        modifier = keySurfaceModifier
    ) {
        val mod = if (crowded) keyTextModifier.padding(top = 8.dp) else keyTextModifier.padding(top = 4.dp)
        Text(
            text = text,
            color = if (selected) bgColor(type) else fgColor(type),
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
    val everythingState by viewModel.everythingState.collectAsStateWithLifecycle()
    val formatState = everythingState.formatState
    fun onClick() {
        // onClick doesn't run at composable time, so need another way to read formatState
        val oldFormat = viewModel.everythingState.value.formatState.numberFormat
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
            KeyButton(text = "⤺", { viewModel.stackRollBack() ?: noMoreUndos() }, Keytype.CONTROL)
            KeyButton(text = "→ϵ", CalcOps.TO_EPS.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "→.", CalcOps.TO_DP.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "h:m", CalcOps.HRMIN.doOp(viewModel), Keytype.BINOP)
            ModalKeyButton(text = "⏱", NumberFormat.TIME, viewModel)
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
            KeyButton(text = "⎣x⎦", CalcOps.FLOOR.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "[x]", CalcOps.ROUND.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "⎡x⎤", CalcOps.CEIL.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "←1", CalcOps.SIGN_EXTEND.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "0→", CalcOps.SIGN_CROP.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "log₁₀", CalcOps.LOG10.doOp(viewModel), Keytype.UNOP, crowded = true)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = btop("sin","-1"), CalcOps.ASIN.doOp(viewModel), Keytype.TRIG, crowded = true)
            KeyButton(text = btop("cos","-1"), CalcOps.ACOS.doOp(viewModel), Keytype.TRIG, crowded = true)
            KeyButton(text = btop("tan","-1"), CalcOps.ATAN.doOp(viewModel), Keytype.TRIG, crowded = true)
            KeyButton(text = "r→⚬", CalcOps.RAD_TO_DEG.doOp(viewModel), Keytype.TRIG)
            KeyButton(text = btop("e","x"), CalcOps.EXP.doOp(viewModel), Keytype.TRIG)
            KeyButton(text = "ln", CalcOps.LN.doOp(viewModel), Keytype.TRIG)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "sin", CalcOps.SIN.doOp(viewModel), Keytype.TRIG)
            KeyButton(text = "cos", CalcOps.COS.doOp(viewModel), Keytype.TRIG)
            KeyButton(text = "tan", CalcOps.TAN.doOp(viewModel), Keytype.TRIG)
            KeyButton(text = "⚬→r", CalcOps.DEG_TO_RAD.doOp(viewModel), Keytype.TRIG)
            KeyButton(text = btop("2","x"), CalcOps.POW2.doOp(viewModel), Keytype.TRIG)
            KeyButton(text = "log₂", CalcOps.LOG2.doOp(viewModel), Keytype.TRIG)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "D", { viewModel.padAppend("d") }, Keytype.ENTRY)
            KeyButton(text = "E", { viewModel.padAppend("e") }, Keytype.ENTRY)
            KeyButton(text = "F", { viewModel.padAppend("f") }, Keytype.ENTRY)
            KeyButton(text = "¬", CalcOps.NOT.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "2×", CalcOps.TIMES2.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "2÷", CalcOps.DIV2.doOp(viewModel), Keytype.UNOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "A", { viewModel.padAppend("a") }, Keytype.ENTRY)
            KeyButton(text = "B", { viewModel.padAppend("b") }, Keytype.ENTRY)
            KeyButton(text = "C", { viewModel.padAppend("c") }, Keytype.ENTRY)
            KeyButton(text = "∧", CalcOps.AND.doOp(viewModel), Keytype.BINOP)
            KeyButton(text = "∨", CalcOps.OR.doOp(viewModel), Keytype.BINOP)
            KeyButton(text = "⨁", CalcOps.XOR.doOp(viewModel), Keytype.BINOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "7", { viewModel.padAppend("7") }, Keytype.ENTRY)
            KeyButton(text = "8", { viewModel.padAppend("8") }, Keytype.ENTRY)
            KeyButton(text = "9", { viewModel.padAppend("9") }, Keytype.ENTRY)
            KeyButton(text = "÷", CalcOps.DIV.doOp(viewModel), Keytype.BINOP)
            KeyButton(text = "mod", CalcOps.MOD.doOp(viewModel), Keytype.BINOP, crowded = true)
            KeyButton(text = "1/x", CalcOps.INV.doOp(viewModel), Keytype.UNOP)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "4", { viewModel.padAppend("4") }, Keytype.ENTRY)
            KeyButton(text = "5", { viewModel.padAppend("5") }, Keytype.ENTRY)
            KeyButton(text = "6", { viewModel.padAppend("6") }, Keytype.ENTRY)
            KeyButton(text = "×", CalcOps.MUL.doOp(viewModel), Keytype.BINOP)
            KeyButton(text = btop("y","x",true), CalcOps.Y_POW_X.doOp(viewModel), Keytype.BINOP)
            KeyButton(text = "E±", { viewModel.padAppendEE() }, Keytype.ENTRY)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "1", { viewModel.padAppend("1") }, Keytype.ENTRY)
            KeyButton(text = "2", { viewModel.padAppend("2") }, Keytype.ENTRY)
            KeyButton(text = "3", { viewModel.padAppend("3") }, Keytype.ENTRY)
            KeyButton(text = "−", CalcOps.SUB.doOp(viewModel), Keytype.BINOP)
            KeyButton(text = "±", CalcOps.CHS.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "π", { viewModel.pushConstant(Math.PI) }, Keytype.ENTRY)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = rowArragement
        ) {
            KeyButton(text = "▲", { viewModel.enterOrDup() }, Keytype.CONTROL)
            KeyButton(text = "0", { viewModel.padAppend("0") }, Keytype.ENTRY)
            KeyButton(text = ".", { viewModel.padAppend(".") }, Keytype.ENTRY)
            KeyButton(text = "+", CalcOps.ADD.doOp(viewModel), Keytype.BINOP)
            KeyButton(text = "√x", CalcOps.SQRT.doOp(viewModel), Keytype.UNOP)
            KeyButton(text = "x⇄y", { viewModel.swap() }, Keytype.CONTROL, crowded = true)
        }
    }
}
