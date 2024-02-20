package com.rtdti.calc16

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CalcViewModelTest {
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
            while (x<1) {
                x++
                emit(FormatTable(x, 3.14+x, x*x, "Float"))
            }
        }
        val stackTableFlow: Flow<List<StackTable>> = flow {
            var x = 2
            while (x<1) {
                x++
                var myList: MutableList<StackTable> = mutableListOf()
                for (i in 0..x-1) {
                    myList.add(StackTable(x, 5, i, x*0.1))
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

    @ExperimentalCoroutinesApi
    val testDispatcher = UnconfinedTestDispatcher()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Dispatchers.Unconfined)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getPadState() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{p->p.pad}.toList(results) }
        viewModel.padAppend("a").join()
        viewModel.padAppend("b").join()
        viewModel.padAppend("c").join()
        viewModel.padBackspace().join()
        viewModel.padBackspace().join()
        viewModel.padBackspace().join()
        viewModel.padBackspace().join()
        viewModel.padAppend("1").join()
        viewModel.padAppendEE().join()
        viewModel.padAppendEE().join()
        viewModel.padAppendEE().join()
        viewModel.padAppend("6").join()
        // System.err.println(results)
        assertEquals("1E+6", results.last())
        job.cancel()
    }

    @Test
    fun padAppend()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{p->p.pad}.toList(results) }
        viewModel.padAppend("0").join()
        viewModel.padAppend("1").join()
        viewModel.padAppend("2").join()
        viewModel.padAppend("3").join()
        viewModel.padAppend("4").join()
        viewModel.padAppend("5").join()
        viewModel.padAppend("6").join()
        viewModel.padAppend("7").join()
        viewModel.padAppend("8").join()
        viewModel.padAppend("9").join()
        viewModel.padAppend("a").join()
        viewModel.padAppend("b").join()
        viewModel.padAppend("c").join()
        viewModel.padAppend("d").join()
        viewModel.padAppend("e").join()
        viewModel.padAppend("f").join()
        assertEquals("0123456789abcdef", results.last())
        job.cancel()
    }

    @Test
    fun padBackspace()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{p->p.pad}.toList(results) }
        viewModel.padBackspace().join()
        viewModel.padBackspace().join()
        assertEquals("", results.last())
        job.cancel()
    }

    @Test
    fun padAppendBackspace1()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{p->p.pad}.toList(results) }
        viewModel.padAppend("0").join()
        viewModel.padAppend("1").join()
        viewModel.padAppend("2").join()
        viewModel.padBackspace().join()
        viewModel.padAppend("4").join()
        viewModel.padBackspace().join()
        viewModel.padBackspace().join()
        viewModel.padAppend("5").join()
        viewModel.padAppend("6").join()
        viewModel.padBackspace().join()
        assertEquals("05", results.last())
        job.cancel()
    }

    @Test
    fun padAppendBackspace2()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{p->p.pad}.toList(results) }
        viewModel.padAppend("0").join()
        viewModel.padBackspace().join()
        viewModel.padAppend("1").join()
        viewModel.padBackspace().join()
        viewModel.padAppend("2").join()
        viewModel.padBackspace().join()
        viewModel.padAppend("3").join()
        viewModel.padBackspace().join()
        assertEquals("", results.last())
        job.cancel()
    }

    @Test
    fun padAppendEE1Pos()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{p->p.pad}.toList(results) }
        viewModel.padAppend("1").join()
        viewModel.padAppendEE().join()
        viewModel.padAppend("6").join()
        assertEquals("1E+6", results.last())
        job.cancel()
    }

    @Test
    fun padAppendEE2Pos()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{p->p.pad}.toList(results) }
        viewModel.padAppend("f").join()
        viewModel.padAppend("e").join()
        viewModel.padAppendEE().join()
        viewModel.padAppend("6").join()
        assertEquals("feE+6", results.last())
        job.cancel()
    }

    @Test
    fun padAppendEE3Pos()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{ p -> p.pad }.toList(results) }
        viewModel.padAppend("1").join()
        viewModel.padAppendEE().join()
        viewModel.padAppendEE().join()
        viewModel.padAppend("3").join()
        assertEquals("1E-3", results.last())
        job.cancel()
    }

    @Test
    fun padAppendEE1Neg()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{ p -> p.pad }.toList(results) }
        viewModel.padAppend("1").join()
        viewModel.padAppendEE().join()
        viewModel.padBackspace().join()
        viewModel.padAppend("3").join()
        assertEquals("1E3", results.last())
        job.cancel()
    }

    @Test
    fun padAppendEE2Neg()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.padState.map{ p -> p.pad }.toList(results) }
        viewModel.padAppend("1").join()
        viewModel.padAppendEE().join()
        viewModel.padBackspace().join()
        viewModel.padAppendEE().join()
        viewModel.padBackspace().join()
        viewModel.padAppend("3").join()
        assertEquals("1EE3", results.last())
        job.cancel()
    }
}
