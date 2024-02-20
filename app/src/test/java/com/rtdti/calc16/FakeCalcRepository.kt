package com.rtdti.calc16

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeCalcRepository : CalcRepository {
    val padQueue = Channel<String>(Channel.UNLIMITED)
    suspend fun padQueueSend(s: String) { padQueue.send(s) }
    val padStringFlow: Flow<String> = flow {
        while (true) {
            emit(padQueue.receive())
        }
    }
    val formatTableQueue = Channel<FormatTable>(Channel.UNLIMITED)
    suspend fun formatTableQueueSend(ft: FormatTable) { formatTableQueue.send(ft) }
    val formatTableFlow: Flow<FormatTable> = flow {
        while (true) {
            emit(formatTableQueue.receive())
        }
    }
    val stackTableQueue = Channel<StackTable>(Channel.UNLIMITED)
    val stackTableList = mutableListOf<StackTable>()
    val stackTableFlow: Flow<List<StackTable>> = flow {
        while (true) {
            val new = stackTableQueue.receive()
            stackTableList.add(new)
            emit(stackTableList)
        }
    }
    override fun getPad(): Flow<String> {
        return padStringFlow
    }
    override suspend fun clearStack() {
        System.err.println("fun clearStack")
    }
    override fun getFormatTable(): Flow<FormatTable> {
        return formatTableFlow
    }
    override fun getStack(): Flow<List<StackTable>> {
        return stackTableFlow
    }
    override suspend fun insertStack(stackTable: StackTable) {
        stackTableQueue.send(stackTable)
    }
    override fun rollbackStack(epoch: Int) {
        System.err.println("rollbackStack")
    }
    override suspend fun insertFullStack(lst: List<StackTable>) {
        for (st in lst) {
            stackTableQueue.send(st)
        }
    }
    override suspend fun insertFullStackClearPad(lst: List<StackTable>) {
        System.err.println("fun insertFullStackClearPad")
    }
    override suspend fun insertOrUpdateFormatTable(formatTable: FormatTable) {
        formatTableQueueSend(formatTable)
    }
    override suspend fun insertOrUpdatePad(pad: String) {
        padQueueSend(pad)
    }
}