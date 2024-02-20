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
    val formatTableFlow: Flow<FormatTable> = flow {
        var x = 0
        while (x < 1) {
            x++
            emit(FormatTable(x, 3.14 + x, x * x, "Float"))
        }
    }
    val stackTableFlow: Flow<List<StackTable>> = flow {
        var x = 2
        while (x < 1) {
            x++
            var myList: MutableList<StackTable> = mutableListOf()
            for (i in 0..x - 1) {
                myList.add(StackTable(x, 5, i, x * 0.1))
            }
            emit(myList)
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
        System.err.println("fun insertStack")
    }
    override fun rollbackStack(epoch: Int) {
        System.err.println("rollbackStack")
    }
    override suspend fun insertFullStack(lst: List<StackTable>) {
        System.err.println("fun insertFullStack")
    }
    override suspend fun insertFullStackClearPad(lst: List<StackTable>) {
        System.err.println("fun insertFullStackClearPad")
    }
    override suspend fun insertOrUpdateFormatTable(formatTable: FormatTable) {
        System.err.println("fun insertOrUpdateFormatTable")
    }
    override suspend fun insertOrUpdatePad(pad: String) {
        padQueueSend(pad)
    }
}