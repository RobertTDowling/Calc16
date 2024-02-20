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

class CalcViewModelStackTest {

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
    fun getStackState() {
    }

    @Test
    fun pushConstant() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        viewModel.pushConstant(3.14).join()
        viewModel.pushConstant(2.78).join()
        System.err.println(results)
        // assertEquals(StackState"1E+6", results.last())
        job.cancel()
    }

    @Test
    fun popToEmpty() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        viewModel.pushConstant(99.9).join()
        viewModel.pop1op({_ -> Unit }).join()
        System.err.println(results)
        // assertEquals(StackState"1E+6", results.last())
        job.cancel()
    }

}