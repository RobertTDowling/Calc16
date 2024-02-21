package com.rtdti.calc16

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

class CalcViewModelPadTest {

    // @ExperimentalCoroutinesApi
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
        viewModel.padBackspace().join()
        viewModel.padBackspace().join()
        assertEquals("", results.last())
        job.cancel()
    }

    @Test
    fun padAppendBackspace1()  = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
        val job = launch(testDispatcher) { viewModel.everythingState.map{it.padState.pad}.toList(results) }
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
