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
    fun pushConstant() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=3.14
        val K2=2.78
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        // System.err.println(results)
        assertEquals(listOf<Double>(), results.first().stackState.stack)
        assertEquals(listOf(K1), results[1].stackState.stack)
        assertEquals(listOf(K2,K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun popToEmpty() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=99.9
        viewModel.pushConstant(K1).join()
        viewModel.pop1op({_ -> Unit }).join()
        assertEquals(listOf<Double>(), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun popToPastEmpty() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=99.9
        viewModel.pushConstant(K1).join()
        viewModel.pop1op({_ -> Unit }).join()
        viewModel.pop1op({_ -> Unit }).join()
        assertEquals(listOf<Double>(), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun pop3To2() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=111.0
        val K2=22.0
        val K3=3.0
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        viewModel.pushConstant(K3).join()
        viewModel.pop1op({_ -> Unit }).join()
        // System.err.println(results)
        assertEquals(listOf(K2, K1), results.last().stackState.stack)
        job.cancel()
    }


    @Test
    fun swap1() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=111.0
        val K2=22.0
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        viewModel.swap().join()
        assertEquals(listOf(K1, K2), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun swap2() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=111.0
        val K2=22.0
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        viewModel.swap().join()
        viewModel.swap().join()
        assertEquals(listOf(K2, K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun swap3() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=111.0
        val K2=22.0
        val K3=3.0
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        viewModel.pushConstant(K3).join()
        viewModel.swap().join()
        assertEquals(listOf(K2, K3, K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun swap4neg() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=111.0
        viewModel.pushConstant(K1).join()
        viewModel.swap().join()
        assertEquals(listOf(K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun swap5neg() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        viewModel.swap().join()
        assertEquals(listOf<Double>(), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun pick1() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=111.0
        val K2=22.0
        val K3=3.0
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        viewModel.pushConstant(K3).join()
        viewModel.pick(1).join() // 22,3,22,111
        viewModel.pick(3).join() // 111,22,3,22,111
        assertEquals(listOf(K1,K2,K3,K2,K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun pick2neg() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=111.0
        val K2=22.0
        val K3=3.0
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        viewModel.pushConstant(K3).join()
        viewModel.pick(4).join() // Error, off end
        assertEquals(listOf(K3,K2,K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun pick3neg() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1=111.0
        viewModel.pushConstant(K1).join()
        viewModel.pick(-1).join() // Error, off beginning
        assertEquals(listOf(K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun unop1() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1 = 111.0
        val K2 = 22.0
        val op = { x: Double -> 2*x }
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        viewModel.unop(op).join()
        assertEquals(listOf(2*K2, K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun binop1() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1 = 111.0
        val K2 = 22.0
        val op = { x: Double, y: Double -> x-y }
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join()
        viewModel.binop(op).join()
        assertEquals(listOf(K1-K2), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun rollback1() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1 = 111.0
        val K2 = 22.0
        val op = { x: Double, y: Double -> x/y }
        viewModel.pushConstant(K1).join()
        viewModel.pushConstant(K2).join() // So far: K2, K1
        viewModel.binop(op).join()        // Divide: K1/K2
        val res = viewModel.stackRollBack() // Undo the division, so should see K2, K1 again
        res?.join()
        assertNotEquals(null, res)
        assertEquals(listOf(K2, K1), results.last().stackState.stack)
        job.cancel()
    }

    @Test
    fun rollback2neg() = runTest {
        val viewModel = CalcViewModel(FakeCalcRepository())
        val results = mutableListOf<CalcViewModel.EverythingState>()
        val job = launch(testDispatcher) { viewModel.everythingState.toList(results) }
        val K1 = 111.0
        viewModel.pushConstant(K1).join() // Push K1
        var res = viewModel.stackRollBack() // Undo the push
        res?.join()
        assertNotEquals(null, res)
        res = viewModel.stackRollBack() // Undo again.... too far
        res?.join()
        assertEquals(null, res)
        // System.err.println(results)
        assertEquals(listOf<Double>(), results.last().stackState.stack)
        job.cancel()
    }
}
