package com.rtdti.calc16

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeCalcRepository : CalcRepository {
    data class EverythingChangeRequest(
        val isPadChange: Boolean,
        val isStackInsert: Boolean,
        val isStackDelete: Boolean,
        val isFormatChange: Boolean,
        val deleteEpoch: Int,
        val pad: String,
        val stackTable: StackTable,
        var formatTable: FormatTable
    )
    val everythingQueue = Channel<EverythingChangeRequest>(Channel.UNLIMITED)
    suspend fun padQueueSend(pad: String) {
        everythingQueue.send(
            EverythingChangeRequest(true, false, false, false,
                0, pad, StackTable(0, 0, 0, 0.0),
                FormatTable(0, 0.0, 0, "FLOAT")))
    }
    suspend fun stackTableInsertQueueSend(stackTable: StackTable) {
        everythingQueue.send(
            EverythingChangeRequest(false, true, false, false,
                0, "", stackTable,
                FormatTable(0, 0.0, 0, "FLOAT")))
    }
    fun stackTableDeleteQueueSend(epoch: Int) {
        everythingQueue.trySend(
            EverythingChangeRequest(false, false, true, false,
                epoch, "", StackTable(0, 0, 0, 0.0),
                FormatTable(0, 0.0, 0, "FLOAT")))
    }
    suspend fun formatTableQueueSend(formatTable: FormatTable) {
        everythingQueue.send(
            EverythingChangeRequest(false, false, false, true,
                0, "", StackTable(0, 0, 0, 0.0),
                formatTable))
    }
    // These things should match CalcViewModel.INIT_EVERYTHING_STATE
    val stackTableEmptyList = StackTable(1, 0, -1, 0.0)
    val stackTableList = mutableListOf<StackTable>(stackTableEmptyList)
    var padTable = String()
    var formatTable = FormatTable(0, 1e-4, 2, "FLOAT")
    val everythingTableFlow: Flow<List<EverythingTable>> = flow {
        while (true) {
            val req = everythingQueue.receive()
            if (req.isPadChange) {
                padTable = req.pad
            }
            if (req.isFormatChange) {
                formatTable = req.formatTable
            }
            if (req.isStackDelete) {
                stackTableList.removeIf { it.epoch == req.deleteEpoch } // Delete only chosen epoch
            }
            if (req.isStackInsert) {
                stackTableList.add(req.stackTable)
            }
            val etl = mutableListOf<EverythingTable>()
            for (st in stackTableList) {
                etl.add(
                    EverythingTable(
                        st.epoch, st.depth, st.value, padTable,
                        formatTable.epsilon, formatTable.decimalPlaces, formatTable.numberFormat))
            }
            // System.err.println(etl)
            emit(etl)
        }
    }
    override suspend fun insertStack(stackTable: StackTable) {
        stackTableInsertQueueSend(stackTable)
    }
    override fun rollbackStack(epoch: Int) {
        stackTableDeleteQueueSend(epoch)
    }
    override suspend fun insertFullStack(lst: List<StackTable>) {
        for (st in lst) {
            stackTableInsertQueueSend(st)
        }
    }
    override suspend fun insertOrUpdateFormatTable(formatTable: FormatTable) {
        formatTableQueueSend(formatTable)
    }
    override suspend fun insertOrUpdatePad(pad: String) {
        padQueueSend(pad)
    }
    override fun getEverythingTable(): Flow<List<EverythingTable>> {
        return everythingTableFlow
    }

}