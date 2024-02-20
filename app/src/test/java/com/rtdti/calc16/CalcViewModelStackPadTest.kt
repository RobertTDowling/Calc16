package com.rtdti.calc16

import kotlinx.coroutines.Dispatchers
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

class CalcViewModelStackPadTest {

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
    fun enterOrDup1() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val results2 = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        val job2 = launch(testDispatcher) { viewModel.padState.map { p -> p.pad }.toList(results2) }
        // Let's type some PI in, then enter it
        viewModel.padAppend("3").join()
        viewModel.padAppend(".").join()
        viewModel.padAppend("1").join()
        val ret = viewModel.enterOrDup()
        assertNotEquals(null, ret)
        ret?.join()
        assertEquals(CalcViewModel.StackState(listOf(3.1)), results.last())
        assertEquals("", results2.last())
        job.cancel()
        job2.cancel()
    }

    @Test
    fun enterOrDup2() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        // Let's just dup a number
        viewModel.pushConstant(3.2).join()
        val ret = viewModel.enterOrDup()
        assertNotEquals(null, ret)
        ret?.join()
        assertEquals(CalcViewModel.StackState(listOf(3.2, 3.2)), results.last())
        job.cancel()
    }

    @Test
    fun enterOrDup3() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        // Enter on empty stack and pad should be NOP
        val ret = viewModel.enterOrDup()
        assertEquals(null, ret)
        ret?.join()
        assertEquals(CalcViewModel.StackState(listOf()), results.last())
        job.cancel()
    }

    @Test
    fun backspaceOrDrop1() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val results2 = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        val job2 = launch(testDispatcher) { viewModel.padState.map { p -> p.pad }.toList(results2) }
        // Let's type some PI in, then delete some of it
        viewModel.padAppend("3").join()
        viewModel.padAppend(".").join()
        viewModel.padAppend("1").join()
        val ret = viewModel.backspaceOrDrop()
        assertNotEquals(null, ret)
        ret?.join()
        assertEquals(CalcViewModel.StackState(listOf()), results.last())
        assertEquals("3.", results2.last())
        job.cancel()
        job2.cancel()
    }

    @Test
    fun backspaceOrDrop2() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        // Let's just drop a number
        val K1=111.0
        val K2=22.0
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        val ret = viewModel.backspaceOrDrop()
        assertNotEquals(null, ret)
        ret?.join()
        assertEquals(CalcViewModel.StackState(listOf(K1)), results.last())
        job.cancel()
    }

    @Test
    fun backspaceOrDrop3() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val results2 = mutableListOf<String>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        val job2 = launch(testDispatcher) { viewModel.padState.map { p -> p.pad }.toList(results2) }
        // Let's type some PI in, then delete all of it
        viewModel.padAppend("3").join()
        viewModel.padAppend(".").join()
        viewModel.padAppend("1").join()
        var ret = viewModel.backspaceOrDrop()
        assertNotEquals(null, ret)
        ret?.join()
        ret = viewModel.backspaceOrDrop()
        assertNotEquals(null, ret)
        ret?.join()
        ret = viewModel.backspaceOrDrop()
        assertNotEquals(null, ret)
        ret?.join()
        ret = viewModel.backspaceOrDrop() // This one should fail
        assertEquals(null, ret)
        ret?.join()
        assertEquals(CalcViewModel.StackState(listOf()), results.last())
        assertEquals("", results2.last())
        job.cancel()
        job2.cancel()
    }
}
