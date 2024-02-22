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
    data class EverythingState(val stackState: StackState, val padState: PadState, val formatState: FormatState,
                               val firstEpoch: Int, val lastEpoch: Int)

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
    // Everything
    //////
    private fun everythingStateFromEverythingTableList(etl: List<EverythingTable>): EverythingState? {
        // What comes in is a List<EverythingTable>
        // Want to create an EverythingState object from that, which contains a list and other
        // things, but is not a list itself
        // System.err.println(stl)
        // Quick out if list is empty
        if (etl.isEmpty()) {
            debugString.value = String.format("E: Empty Flow")
            return INIT_EVERYTHING_STATE
        }
        val sortedEtl =
            etl.sortedBy { it.depth }// .sortedWith(compareBy<StackTable>{ it.epoch }.thenBy{ it.depth })
        val firstEpoch = sortedEtl.minOf { it.epoch }
        val lastEpoch = sortedEtl.maxOf { it.epoch }
        // Filter for only lastEpoch
        // Expect to see entries depth[0..D-1] with values and and depth[-1] holding D
        // Example: If the stack has 2 entries, 3.1 and 4.7, expect
        // depth=0,value=3.1; depth=1,value=4.7; depth=-1,value=2
        val filteredEtl = sortedEtl.filter { it.epoch == lastEpoch }
        // Sanity check
        if (!filteredEtl.isEmpty()) {
            // System.err.println(String.format("epoch=%d first.d=%d last.d=%d size=%d", filteredStl.first().epoch, filteredStl.first().depth, filteredStl.last().depth, filteredStl.size))
            // [StackTable(rowid=0, epoch=1, depth=-1, value=1.0), StackTable(rowid=0, epoch=1, depth=0, value=99.9)]
            // epoch=1 first.d=-1 last.d=0 size=2
            if (filteredEtl.first().depth != -1
                || filteredEtl.last().depth != filteredEtl.size - 2
                || filteredEtl.first().value.toInt() != filteredEtl.last().depth + 1
            ) {
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
        val lastEt = filteredEtl.last()
        val stackState = StackState(filteredEtl.filter { it.depth >= 0 }.map { it.value })
        val padState = PadState(lastEt.pad)
        val formatState = FormatState(lastEt.epsilon, lastEt.decimalPlaces, NumberFormat.valueOf(lastEt.numberFormat))
        return EverythingState(stackState, padState, formatState, firstEpoch, lastEpoch)
    }
    val INIT_EVERYTHING_STATE = EverythingState(
        StackState(listOf()),
        PadState(""),
        FormatState(1e-4, 2, NumberFormat.FLOAT),
        0,
        0,
    )
    val everythingState = repository.getEverythingTable().mapNotNull { everythingStateFromEverythingTableList(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500L),
            initialValue = INIT_EVERYTHING_STATE
        )
    
    //////
    // Format Parameters
    //////
    fun epsilonSet(epsilon: Double) {
        updateFormatTable(FormatState(epsilon, everythingState.value.formatState.decimalPlaces, everythingState.value.formatState.numberFormat)) }
    fun decimalPlacesSet(decimalPlaces: Int) {
        updateFormatTable(FormatState(everythingState.value.formatState.epsilon, decimalPlaces, everythingState.value.formatState.numberFormat)) }
    fun numberFormatSet(numberFormat: NumberFormat) {
        updateFormatTable(FormatState(everythingState.value.formatState.epsilon, everythingState.value.formatState.decimalPlaces, numberFormat)) }
    fun updateFormatTable(formatState: FormatState) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            val formatTable = FormatTable(1, formatState.epsilon, formatState.decimalPlaces, formatState.numberFormat.toString())
            repository.insertOrUpdateFormatTable(formatTable)
        }
    }
    ////////
    /// Pad
    ////////
    fun padIsEmpty() = everythingState.value.padState.pad.isEmpty()
    fun padAppend(char: String) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            repository.insertOrUpdatePad(everythingState.value.padState.pad.plus(char))
        }
    }
    fun padAppendEE() = viewModelScope.launch {
        var pad: String = everythingState.value.padState.pad
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
                repository.insertOrUpdatePad(everythingState.value.padState.pad.substring(0, everythingState.value.padState.pad.length - 1))
            }
        }
    }

    ////////
    /// Stack
    ////////
    private val stackFirstEpoch = mutableStateOf(0)
    private val stackLastEpoch = mutableStateOf(0)
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
        val epoch = stackLastEpoch.value + 1
        pruneBackups(epoch)
        repository.insertFullStack(workingStack.asListStackTable(epoch))
    }

    // In preparation for doing a stack operation, copy pad to top of stack and clear pad
    // Return the modified working stack for the actual operation to work on
    private suspend fun doEnterThenGetWorkingStack(): WorkingStack {
        val x = try {
            val formatter = Formatter(everythingState.value.formatState, 0)
            formatter.parser(everythingState.value.padState.pad)
        } catch (e: Exception) {
            0.0 // Double.NaN // SQLite barfs on this
        }
        val workingStack = WorkingStack(everythingState.value.stackState)
        workingStack.push(x)
        val epoch = stackLastEpoch.value + 1
        pruneBackups(epoch)
        repository.insertFullStack(workingStack.asListStackTable(epoch))
        repository.insertOrUpdatePad("")
        stackLastEpoch.value = epoch
        return workingStack
    }

    private suspend fun doImpliedEnterThenGetWorkingStack(): WorkingStack {
        // Only doEnter if pad isn't empty
        if (!padIsEmpty()) {
            return doEnterThenGetWorkingStack()
        }
        val workingStack = WorkingStack(everythingState.value.stackState)
        return workingStack
    }

    private fun Enter() = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            doEnterThenGetWorkingStack()
        }
    }

    fun pushConstant(x: Double) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            val workingStack = doImpliedEnterThenGetWorkingStack()
            workingStack.push(x)
            backupStack(workingStack)
        }
    }

    fun swap() = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            val workingStack = doImpliedEnterThenGetWorkingStack()
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
            val workingStack = doImpliedEnterThenGetWorkingStack()
            workingStack.pick(index)
            backupStack(workingStack)
        }
    }
    fun binop(op: (Double, Double) -> Double) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            val workingStack = doImpliedEnterThenGetWorkingStack()
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
            val workingStack = doImpliedEnterThenGetWorkingStack()
            if (workingStack.hasDepth(1)) {
                val a = workingStack.pop()
                workingStack.push(op(a))
                backupStack(workingStack)
            }
        }
    }

    fun pop1op(op: (Double) -> Unit) = viewModelScope.launch {
        withContext(repositoryDispatcher) {
            val workingStack = doImpliedEnterThenGetWorkingStack()
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

    // Combo backspace and drop
    // Return null if nothing was done (pad was clear and stack was empty.. nothing to drop)
    fun backspaceOrDrop(): Job? {
        if (padIsEmpty()) {
            if (!everythingState.value.stackState.stack.isEmpty()) {
                return pop1op({ d -> })
            }
        } else {
            return padBackspace()
        }
        return null
    }

    // Combo enter and dup.
    // Return null if nothing was done (pad was clear and stack was empty.. nothing to dup)
    fun enterOrDup(): Job? {
        if (!padIsEmpty()) {
            return Enter()
        } else {
            if (!everythingState.value.stackState.stack.isEmpty()) {
                val a = everythingState.value.stackState.stack.first();
                return pushConstant(a)
            }
        }
        return null
    }
}
