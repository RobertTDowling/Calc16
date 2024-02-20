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
        val K1=3.14
        val K2=2.78
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        // System.err.println(results)
        assertEquals(CalcViewModel.StackState(listOf<Double>()), results.first())
        assertEquals(CalcViewModel.StackState(listOf<Double>(K1)), results[1])
        assertEquals(CalcViewModel.StackState(listOf<Double>(K2,K1)), results.last())
        job.cancel()
    }

    @Test
    fun popToEmpty() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.StackState>()
        val job = launch(testDispatcher) { viewModel.stackState.toList(results) }
        val K1=99.9
        viewModel.pushConstant(K1).join()
        viewModel.pop1op({_ -> Unit }).join()
        // System.err.println(results)
        assertEquals(CalcViewModel.StackState(listOf<Double>()), results.first())
        job.cancel()
    }

}