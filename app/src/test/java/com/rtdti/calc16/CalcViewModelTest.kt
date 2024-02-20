package com.rtdti.calc16

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

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
            System.err.println("getPad")
            return padStringFlow
        }
        override suspend fun clearStack() {
            System.err.println("fun clearStack")
        }
        override fun getFormatTable(): Flow<FormatTable> {
            System.err.println("getFormatTable")
            return formatTableFlow
        }
        override fun getStack(): Flow<List<StackTable>> {
            System.err.println("getStack")
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
            // System.err.println("fun insertOrUpdatePad")
        }
    }

    @ExperimentalCoroutinesApi
    val testDispatcher = UnconfinedTestDispatcher()
    // val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getPadState() = runTest {
        // val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = FakeCalcRepository()
        val viewModel = CalcViewModel(repository)
        val results = mutableListOf<CalcViewModel.PadState>()
        val job = launch(testDispatcher) { viewModel.padState.toList(results) }
        // val job = launch(testDispatcher) { results.add(viewModel.padState.last()) }
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
        System.err.println(results)
        job.cancel()
    }

    /*
    @Test
    fun padAppend() {
    }

    @Test
    fun padAppendEE() {
    }

    @Test
    fun padBackspace() {
    }
    */
}
