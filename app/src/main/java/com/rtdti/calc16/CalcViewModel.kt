package com.rtdti.calc16

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalcViewModel(private val repository: CalcRepository) : ViewModel() {
    data class PadState(val pad: String)
    data class StackState(val stack: List<Double>)
    data class FormatState(val epsilon: Double, val decimalPlaces: Int, val numberFormat: NumberFormat)

    // fun PadState.isEmpty() : Boolean = pad.isEmpty()

    class WorkingStack(stackState: StackState) {
        val stack: MutableList<Double> = stackState.stack.toMutableList()
        fun asListStackTable(epoch: Int) : List<StackTable> {
            if (stack.isEmpty()) {
                return listOf(StackTable(0, epoch, -1, 0.0))
            }
            return stack.mapIndexed { depth, value -> StackTable(0, epoch, depth, value)}
        }
        fun hasDepth(depth: Int) = stack.size >= depth
        fun isEmpty() = !hasDepth(1)
        fun push(x: Double) = stack.add(0, x)
        fun pop(): Double = stack.removeAt(0)
        fun pick(depth: Int) = push(stack[depth])
    }

    val debugString = mutableStateOf("")
    //////
    // Format Parameters
    //////
    val formatState = repository.getFormatTable().mapNotNull { it?.let {FormatState(it.epsilon, it.decimalPlaces, NumberFormat.valueOf(it.numberFormat))} }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500L),
            initialValue = FormatState(1e-4, 2, NumberFormat.FLOAT)
        )
    fun epsilonSet(epsilon: Double) {
        updateFormatTable(FormatState(epsilon, formatState.value.decimalPlaces, formatState.value.numberFormat)) }
    fun decimalPlacesSet(decimalPlaces: Int) {
        updateFormatTable(FormatState(formatState.value.epsilon, decimalPlaces, formatState.value.numberFormat)) }
    fun numberFormatSet(numberFormat: NumberFormat) {
        updateFormatTable(FormatState(formatState.value.epsilon, formatState.value.decimalPlaces, numberFormat)) }
    fun numberFormatGet() : NumberFormat { return formatState.value.numberFormat }
    fun updateFormatTable(formatState: FormatState) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val formatTable = FormatTable(0, formatState.epsilon, formatState.decimalPlaces, formatState.numberFormat.toString())
            repository.insertOrUpdateFormatTable(formatTable)
        }
    }
    ////////
    /// Pad
    ////////
    val padState = repository.getPad().map { PadState(it ?: "") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500L),
            initialValue = PadState("")
        )

    fun padIsEmpty() = padState.value.pad.isEmpty()
    fun padAppend(char: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insertOrUpdatePad(padState.value.pad.plus(char))
        }
    }

    fun padBackspace() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            if (!padIsEmpty()) {
                repository.insertOrUpdatePad(padState.value.pad.substring(0, padState.value.pad.length - 1))
            }
        }
    }

    ////////
    /// Stack
    ////////
    val stackFirstEpoch = mutableStateOf(0)
    val stackLastEpoch = mutableStateOf(0)
    fun stackStateFromStackTableList(stl: List<StackTable>): StackState? {
        // Quick out if list is empty
        if (stl.isEmpty()) {
            debugString.value = String.format("E: Empty Flow")
            return null
        }
        val sortedStl = stl.sortedBy { it.depth }// .sortedWith(compareBy<StackTable>{ it.epoch }.thenBy{ it.depth })
        val firstEpoch = sortedStl.minOf { it.epoch }
        val lastEpoch = sortedStl.maxOf { it.epoch }
        // Filter for only lastEpoch
        val filteredStl = sortedStl.filter { it.epoch == lastEpoch }.filter { it.depth >= 0 }
        // Sanity check
        if (!filteredStl.isEmpty() && (filteredStl.first().depth > 0 || filteredStl.last().depth != filteredStl.size - 1)) {
            // we are in trouble
            debugString.value = String.format("E: %d..%d Invalid", firstEpoch, lastEpoch, filteredStl.size)
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.clearStack()
                }
            }
            return null
        }
        stackLastEpoch.value = lastEpoch // FIXME: Seems this should be bundled into StackState
        stackFirstEpoch.value = firstEpoch
        debugString.value = String.format("E: %d..%d D: %d", firstEpoch, lastEpoch, filteredStl.size)
        return StackState(filteredStl.map { it.value })
    }

    val stackState = repository.getStack().mapNotNull { stackStateFromStackTableList(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StackState(listOf())
        )

    fun stackDepth() : Int = stackState.value.stack.size
    fun stackRollBack() : Boolean {
        val epoch = stackLastEpoch.value
        if (epoch > stackFirstEpoch.value) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.rollbackStack(epoch)
                }
            }
            return false
        } else {
            return true
        }
    }

    private suspend fun pruneBackups(epoch: Int) {
        val firstEpoch = stackFirstEpoch.value
        if (firstEpoch + 30 < epoch) {
            repository.rollbackStack(firstEpoch)
        }
    }

    private suspend fun backupStack(workingStack: WorkingStack) {
        val epoch = stackLastEpoch.value + 1
        pruneBackups(epoch)
        repository.insertFullStack(workingStack.asListStackTable(epoch))
    }

    private suspend fun doEnter(): WorkingStack {
        // Copy pad to top of stack and clear pad
        val x = try {
            if (numberFormatGet() == NumberFormat.HEX) {
                padState.value.pad.toLong(radix = 16).toDouble()
            } else {
                padState.value.pad.toDouble()
            }
        } catch (e: Exception) {
            0.0
        }
        val workingStack = WorkingStack(stackState.value)
        workingStack.push(x)
        val epoch = stackLastEpoch.value + 1
        pruneBackups(epoch)
        repository.insertFullStackClearPad(workingStack.asListStackTable(epoch))
        stackLastEpoch.value = epoch
        return workingStack
    }
    private suspend fun doImpliedEnter(): WorkingStack {
        // Only doEnter if pad isn't empty
        if (!padIsEmpty()) {
            return doEnter()
        }
        val workingStack = WorkingStack(stackState.value)
        return workingStack
    }

    private fun Enter() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            doEnter()
        }
    }

    fun pushConstant(x: Double) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
            workingStack.push(x)
            backupStack(workingStack)
        }
    }

    fun swap() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
            if (workingStack.hasDepth(2)) {
                val b = workingStack.pop()
                val a = workingStack.pop()
                workingStack.push(b)
                workingStack.push(a)
                backupStack(workingStack)
            }
        }
    }

    fun pick(index: Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
            workingStack.pick(index)
            backupStack(workingStack)
        }
    }
    fun binop(op: (Double, Double) -> Double) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
            if (workingStack.hasDepth(2)) {
                val b = workingStack.pop()
                val a = workingStack.pop()
                workingStack.push(op(a, b))
                backupStack(workingStack)
            }
        }
    }

    fun unop(op: (Double) -> Double) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
            if (workingStack.hasDepth(1)) {
                val a = workingStack.pop()
                workingStack.push(op(a))
                backupStack(workingStack)
            }
        }
    }

    fun pop1op(op: (Double) -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val workingStack = doImpliedEnter()
            if (workingStack.hasDepth(1)) {
                val a = workingStack.pop()
                op(a)
                backupStack(workingStack)
            }
        }
    }

    /////////
    // Pad + Stack
    /////////
    fun backspaceOrDrop() { // Combo backspace and drop
        if (padIsEmpty()) {
            if (!stackState.value.stack.isEmpty()) {
                pop1op({ d -> })
            }
        } else {
            padBackspace()
        }
    }

    fun enterOrDup() { // Combo enter and dup
        if (!padIsEmpty()) {
            Enter()
        } else {
            if (!stackState.value.stack.isEmpty()) {
                val a = stackState.value.stack.first();
                pushConstant(a)
            }
        }
    }
}
