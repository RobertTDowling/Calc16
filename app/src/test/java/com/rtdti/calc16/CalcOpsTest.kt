package com.rtdti.calc16

import org.junit.Assert.*

import org.junit.Test

class CalcOpsTest {

    @Test
    fun doOp() {
        // These tests require that we have a viewModel mocked or working, and a stack, and we'd
        // have to verify the stack changed.  The actual operations are hidden from caller.
        // So develop these once we have a viewModel we can test
        /*
        assertEquals(Math.PI/6, CalcOps.ACOS.doOp(0.5)()
        CalcOps.ACOS -> fun() { viewModel.unop({ a -> Math.acos(a) }) }
        CalcOps.ADD -> fun() { viewModel.binop({ a, b -> CalcMath.add(a,b) }) }
        CalcOps.AND -> fun() { viewModel.binop({ a, b -> CalcMath.and(a,b) }) }
        CalcOps.ASIN -> fun() { viewModel.unop({ a -> Math.asin(a) }) }
        CalcOps.ATAN -> fun() { viewModel.unop({ a -> Math.atan(a) }) }
        CalcOps.CEIL -> fun() { viewModel.unop({ a -> Math.ceil(a) }) }
        CalcOps.CHS -> fun() { viewModel.unop({ a -> CalcMath.chs(a) }) }
        CalcOps.COS -> fun() { viewModel.unop({ a -> Math.cos(a) }) }
        CalcOps.DEG_TO_RAD -> fun() { viewModel.unop({ a -> CalcMath.degToRad(a) }) }
        CalcOps.DIV -> fun() { viewModel.binop({ a, b -> CalcMath.div(a,b) }) }
        CalcOps.DIV2 -> fun() { viewModel.unop({ a -> CalcMath.div2(a) }) }
        CalcOps.DP_FROM -> fun() { viewModel.pushConstant(viewModel.formatState.value.decimalPlaces.toDouble()) }
        CalcOps.EE -> fun() { viewModel.padAppendEE() }
        CalcOps.EPS_FROM -> fun() { viewModel.pushConstant(viewModel.formatState.value.epsilon) }
        CalcOps.EXP -> fun() { viewModel.unop({ a -> Math.exp(a) }) }
        CalcOps.FLOOR -> fun() { viewModel.unop({ a -> Math.floor(a) }) }
        CalcOps.GCD -> fun() { viewModel.binop({ a, b -> CalcMath.gcd(a.toLong(),b.toLong()).toDouble() }) }
        CalcOps.INV -> fun() { viewModel.unop({ a -> CalcMath.inv(a) }) }
        CalcOps.LCM -> fun() { viewModel.binop({ a, b -> CalcMath.lcm(a.toLong(),b.toLong()).toDouble() }) }
        CalcOps.LN -> fun() { viewModel.unop({ a -> Math.log(a) }) }
        CalcOps.LOG10 -> fun() { viewModel.unop({ a -> CalcMath.log10(a) }) }
        CalcOps.LOG2 -> fun() { viewModel.unop({ a -> CalcMath.log2(a) }) }
        CalcOps.MOD -> fun() { viewModel.binop({ a, b -> CalcMath.mod(a,b) }) }
        CalcOps.MUL -> fun() { viewModel.binop({ a, b -> CalcMath.mul(a,b) }) }
        CalcOps.NOT -> fun() { viewModel.unop({ a -> CalcMath.not(a) }) }
        CalcOps.OR -> fun() { viewModel.binop({ a, b -> CalcMath.or(a,b) }) }
        CalcOps.PI -> fun() { viewModel.pushConstant(Math.PI) }
        CalcOps.POW10 -> fun() { viewModel.unop({ a -> CalcMath.pow10(a) }) }
        CalcOps.POW2 -> fun() { viewModel.unop({ a -> CalcMath.pow2(a) }) }
        CalcOps.RAD_TO_DEG -> fun() { viewModel.unop({ a -> CalcMath.radTodDeg(a) }) }
        CalcOps.ROUND -> fun() { viewModel.unop({ a -> Math.round(a).toDouble() }) }
        CalcOps.SIGN_CROP -> fun() { viewModel.unop({ a -> CalcMath.signCrop(a) }) }
        CalcOps.SIGN_EXTEND -> fun() { viewModel.unop({ a -> CalcMath.signExtend(a) }) }
        CalcOps.SIN -> fun() { viewModel.unop({ a -> Math.sin(a) }) }
        CalcOps.SQRT -> fun() { viewModel.unop({ a -> Math.sqrt(a) }) }
        CalcOps.SUB -> fun() { viewModel.binop({ a, b -> CalcMath.sub(a,b) }) }
        CalcOps.TAN -> fun() { viewModel.unop({ a -> Math.tan(a) }) }
        CalcOps.TIMES2 -> fun() { viewModel.unop({ a -> CalcMath.times2(a) }) }
        CalcOps.TO_DP -> fun() { viewModel.pop1op({ d -> viewModel.decimalPlacesSet(d.toInt()) }) }
        CalcOps.TO_EPS -> fun() { viewModel.pop1op({ e -> viewModel.epsilonSet(e) }) }
        CalcOps.XOR -> fun() { viewModel.binop({ a, b -> CalcMath.xor(a,b) }) }
        CalcOps.Y_POW_X -> fun() { viewModel.binop({ a, b -> Math.pow(a,b) }) }
        */
    }
}
