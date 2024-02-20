package com.rtdti.calc16

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class CalcViewModel(private val repository: CalcRepository,
                         val repositoryDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    data class PadState(val pad: String)
    data class StackState(val stack: List<Double>)
    data class FormatState(val epsilon: Double, val decimalPlaces: Int, val numberFormat: NumberFormat)

    // fun PadState.isEmpty() : Boolean = pad.isEmpty()

    private class WorkingStack(stackState: StackState) {
        val stack: MutableList<Double> = stackState.stack.toMutableList()
        fun asListStackTable(epoch: Int) : List<StackTable> {
            // Expect to see entries depth[0..D-1] with values and and depth[-1] holding D
            // Example: If the stack has 2 entries, 3.1 and 4.7, expect
            // depth=0,value=3.1; depth=1,value=4.7; depth=-1,value=2
            var ret = stack.mapIndexed { depth, value -> StackTable(0, epoch, depth, value)}
                .toMutableList()
            ret.add(StackTable(0, epoch, -1, stack.size.toDouble()))
            return ret
        }
        fun hasDepth(depth: Int) = stack.size >= depth
        fun isEmpty() = !hasDepth(1)
        fun push(x: Double) = stack.add(0, x)
        fun pop(): Double = stack.removeAt(0)
        fun pick(depth: Int) = try { push(stack[depth]) } catch (_: IndexOutOfBoundsException) {}
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
    fun updateFormatTable(formatState: FormatState) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
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
        withContext(repositoryDispatcher) {
            repository.insertOrUpdatePad(padState.value.pad.plus(char))
        }
    }
    fun padAppendEE() = viewModelScope.launch {
        var pad: String = padState.value.pad
        if (pad.endsWith("E+")) {
            pad = pad.removeSuffix("E+").plus("E-")
        } else if (pad.endsWith("E-")) {
            pad = pad.removeSuffix("E-").plus("E+")
        } else {
            pad = pad.plus("E+")
        }
        withContext(repositoryDispatcher) {
            repository.insertOrUpdatePad(pad)
        }
    }

    fun padBackspace() = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            if (!padIsEmpty()) {
                repository.insertOrUpdatePad(padState.value.pad.substring(0, padState.value.pad.length - 1))
            }
        }
    }

    ////////
    /// Stack
    ////////
    private val stackFirstEpoch = mutableStateOf(0)
    private val stackLastEpoch = mutableStateOf(-1) // <0 Flag: no previous epochs
    private fun stackStateFromStackTableList(stl: List<StackTable>): StackState? {
        // System.err.println(stl)
        // Quick out if list is empty
        if (stl.isEmpty()) {
            debugString.value = String.format("E: Empty Flow")
            return null
        }
        val sortedStl = stl.sortedBy { it.depth }// .sortedWith(compareBy<StackTable>{ it.epoch }.thenBy{ it.depth })
        val firstEpoch = sortedStl.minOf { it.epoch }
        val lastEpoch = sortedStl.maxOf { it.epoch }
        // Filter for only lastEpoch
        // Expect to see entries depth[0..D-1] with values and and depth[-1] holding D
        // Example: If the stack has 2 entries, 3.1 and 4.7, expect
        // depth=0,value=3.1; depth=1,value=4.7; depth=-1,value=2
        val filteredStl = sortedStl.filter { it.epoch == lastEpoch }
        // Sanity check
        if (!filteredStl.isEmpty()) {
            // System.err.println(String.format("epoch=%d first.d=%d last.d=%d size=%d", filteredStl.first().epoch, filteredStl.first().depth, filteredStl.last().depth, filteredStl.size))
            // [StackTable(rowid=0, epoch=1, depth=-1, value=1.0), StackTable(rowid=0, epoch=1, depth=0, value=99.9)]
            // epoch=1 first.d=-1 last.d=0 size=2
            if (filteredStl.first().depth != -1
                || filteredStl.last().depth != filteredStl.size - 2
                || filteredStl.first().value.toInt() != filteredStl.last().depth+1) {
                // we are in trouble
                debugString.value = String.format("Invalid Epoch: %d..%d", firstEpoch, lastEpoch)
                return null
            } else {
                // System.err.println("Valid -----------------")
            }
        }
        stackLastEpoch.value = lastEpoch // FIXME: Seems this should be bundled into StackState
        stackFirstEpoch.value = firstEpoch
        debugString.value = String.format("Undo Epochs: %d..%d", firstEpoch, lastEpoch)
        return StackState(filteredStl.filter { it.depth >= 0 }.map { it.value })
    }

    val stackState = repository.getStack().mapNotNull { stackStateFromStackTableList(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StackState(listOf())
        )

    fun stackDepth() : Int = stackState.value.stack.size
    fun stackRollBack() : Job? {
        val epoch = stackLastEpoch.value
        if (epoch > stackFirstEpoch.value) {
            return viewModelScope.launch {
                withContext(repositoryDispatcher) {
                    repository.rollbackStack(epoch)
                }
            }
        } else {
            return null // No rollbacks possible
        }
    }

    private suspend fun pruneBackups(epoch: Int) {
        val firstEpoch = stackFirstEpoch.value
        val HISTORY_DEPTH = 30
        if (firstEpoch + HISTORY_DEPTH < epoch) {
            repository.rollbackStack(firstEpoch)
        }
    }

    private suspend fun backupStack(workingStack: WorkingStack) {
        var epoch = stackLastEpoch.value + 1
        // Check for no previous epochs and back up an empty stack
        if (epoch == 0) {
            // System.err.print("Backup first (empty)")
            val emptyStack = WorkingStack(StackState(listOf()))
            repository.insertFullStack(emptyStack.asListStackTable(epoch))
            epoch += 1
        }
        pruneBackups(epoch)
        repository.insertFullStack(workingStack.asListStackTable(epoch))
    }

    private suspend fun doEnter(): WorkingStack {
        // Copy pad to top of stack and clear pad
        val x = try {
            val formatter = Formatter(formatState.value, 0)
            formatter.parser(padState.value.pad)
        } catch (e: Exception) {
            0.0 // Double.NaN // SQLite barfs on this
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
        withContext(repositoryDispatcher) {
            doEnter()
        }
    }

    fun pushConstant(x: Double) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            val workingStack = doImpliedEnter()
            workingStack.push(x)
            backupStack(workingStack)
        }
    }

    fun swap() = viewModelScope.launch {
        withContext(repositoryDispatcher) {
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
        withContext(repositoryDispatcher) {
            val workingStack = doImpliedEnter()
            workingStack.pick(index)
            backupStack(workingStack)
        }
    }
    fun binop(op: (Double, Double) -> Double) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
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
        withContext(repositoryDispatcher) {
            val workingStack = doImpliedEnter()
            if (workingStack.hasDepth(1)) {
                val a = workingStack.pop()
                workingStack.push(op(a))
                backupStack(workingStack)
            }
        }
    }

    fun pop1op(op: (Double) -> Unit) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
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
