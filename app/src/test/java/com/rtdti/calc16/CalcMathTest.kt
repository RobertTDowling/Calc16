package com.rtdti.calc16

import org.junit.Assert.*

import org.junit.Test

class CalcMathTest {
    val EPSILON = 1e-4

    @Test
    fun frac() {
        // Compare components
        assertEquals(Frac(11L, 4L, 0.0), Frac(11L, 4L, 0.0))
        assertNotEquals(Frac(12L, 4L, 0.0), Frac(11L, 4L, 0.0))
        assertNotEquals(Frac(11L, 5L, 0.0), Frac(11L, 4L, 0.0))
        assertNotEquals(Frac(11L, 4L, 0.1), Frac(11L, 4L, 0.0))
        // Compare components for near
        assertTrue(Frac(11L, 4L, 0.0).near(Frac(11L, 4L, 0.0), 0.0))
        assertFalse(Frac(12L, 4L, 0.0).near(Frac(11L, 4L, 0.0), 0.0))
        assertFalse(Frac(11L, 5L, 0.0).near(Frac(11L, 4L, 0.0), 0.0))
        assertFalse(Frac(11L, 4L, 0.1).near(Frac(11L, 4L, 0.0), 0.0))
        // Compare errors for near
        assertTrue(Frac(3L, 10L, 0.1).near(Frac(3L, 10L, 0.02),0.11))
        assertTrue(Frac(3L, 10L, 0.02).near(Frac(3L, 10L, 0.1),0.11))
        assertFalse(Frac(3L, 10L, 0.1).near(Frac(3L, 10L, 0.2),0.11))
        assertFalse(Frac(3L, 10L, 0.2).near(Frac(3L, 10L, 0.1),0.11))
        // Check that abs of error is used for near
        assertFalse(Frac(3L, 10L, 0.1).near(Frac(3L, 10L, -0.2),0.11))
        assertFalse(Frac(3L, 10L, -0.2).near(Frac(3L, 10L, 0.1),0.11))
        assertTrue(Frac(3L, 10L, -0.1).near(Frac(3L, 10L, 0.02),0.11))
        assertTrue(Frac(3L, 10L, 0.02).near(Frac(3L, 10L, -0.1),0.11))
        assertTrue(Frac(3L, 10L, -0.02).near(Frac(3L, 10L, -0.02),0.02))
    }

    @Test
    fun double2frac() {
        val MAX_DEN = 1/EPSILON
        assertEquals(Frac(11L, 4L, 0.0), CalcMath.double2frac(2.75, MAX_DEN))
        assertEquals(Frac(0L, 1L, 0.0), CalcMath.double2frac(0.0, MAX_DEN))
        assertEquals(Frac(1L, 1L, 0.0), CalcMath.double2frac(1.0, MAX_DEN))
        assertEquals(Frac(-2L, 1L, 0.0), CalcMath.double2frac(-2.0, MAX_DEN))
        assertEquals(Frac(22L, 7L, 0.0), CalcMath.double2frac(22/7.0, MAX_DEN))
        assertTrue(Frac(22L, 7L, 0.0).near(CalcMath.double2frac(22/7.0, MAX_DEN), EPSILON))
        assertNotEquals(Frac(355L, 113L, 0.0), CalcMath.double2frac(Math.PI, MAX_DEN))
        assertTrue(Frac(355L, 113L, 0.0).near(CalcMath.double2frac(Math.PI, MAX_DEN), EPSILON))
        // assertTrue(Frac(22L, 7L, 0.0).near(CalcMath.double2frac(Math.PI, 0.001), 0.001))
        val E2 = 10.0
        System.err.println(E2)
        System.err.println(CalcMath.double2frac(Math.PI, E2))
        val E3 = 100.0
        System.err.println(E3)
        System.err.println(CalcMath.double2frac(Math.PI, E3))
        val E4 = 1000.0
        System.err.println(E4)
        System.err.println(CalcMath.double2frac(Math.PI, E4))
        assertFalse(Frac(22L, 7L, 0.0).near(CalcMath.double2frac(Math.PI, E2), EPSILON))
        assertTrue(Frac(22L, 7L, 0.0).near(CalcMath.double2frac(Math.PI, E2), 1/E2))
    }

    @Test
    fun double2imperial() {
    }

    @Test
    fun primeFactorAnnotatedString() {
    }

    @Test
    fun gcd() {
    }

    @Test
    fun lcm() {
    }
}