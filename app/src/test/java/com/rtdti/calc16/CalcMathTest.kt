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
        fun twoto(e: Int) = Math.pow(2.0, e.toDouble()).toLong()
        fun primeFactorString(x: Long): String {
            val str = CalcMath.primeFactorAnnotatedString(x, 0)
            return str.toString()
        }
        assertEquals("0", primeFactorString(0L))
        assertEquals("1", primeFactorString(1L))
        assertEquals("2", primeFactorString(2L))
        assertEquals("3", primeFactorString(3L))
        assertEquals("2^2", primeFactorString(4L))
        assertEquals("-5", primeFactorString(-5L))
        assertEquals("-2*3", primeFactorString(-6L))
        assertEquals("2*3*5", primeFactorString(2*3*5L))
        assertEquals("2^20", primeFactorString(twoto(20)))
        assertNotEquals("3^20", primeFactorString(twoto(20)))
        assertNotEquals("2^21", primeFactorString(twoto(20)))
        assertNotEquals("2^19", primeFactorString(twoto(20)))
        assertEquals("2^10*5^10", primeFactorString(10000000000))
        assertEquals((twoto(31)-1).toString(), primeFactorString(twoto(31)-1))
        assertEquals("641*6700417*2147483647", primeFactorString((twoto(31)-1) * (twoto(32)+1)))
    }

    @Test
    fun gcd() {
        assertEquals(2, CalcMath.gcd(6, 8))
        assertEquals(2, CalcMath.gcd(-6, 8))
        assertEquals(2, CalcMath.gcd(6, -8))
        assertEquals(2, CalcMath.gcd(-6, -8))
        assertEquals(22, CalcMath.gcd(66, 88))
        assertEquals(1, CalcMath.gcd(355, 113))
        assertEquals(1, CalcMath.gcd(355, 1))
        assertEquals(1, CalcMath.gcd(1, 113))
        assertEquals(1, CalcMath.gcd(0, 0))
        assertEquals(1, CalcMath.gcd(355, 0))
        assertEquals(1, CalcMath.gcd(0, 113))
    }

    @Test
    fun lcm() {
        assertEquals(15, CalcMath.lcm(3, 5))
        assertEquals(24, CalcMath.lcm(6, 8))
    }
    
    @Test
    fun timeString() {
        assertEquals("0:00", CalcMath.timeString(0.0))
        assertEquals("1:30", CalcMath.timeString(1.5))
        assertEquals("1:30", CalcMath.timeString(1.501))
        assertEquals("-1:15", CalcMath.timeString(-1.25))
        assertEquals("0:00", CalcMath.timeString(0.4/60))
        assertEquals("0:01", CalcMath.timeString(0.6/60))
    }

    @Test
    fun floatString()
    {
        fun twoto(e: Int) = Math.pow(2.0, e.toDouble())
        assertEquals("0", CalcMath.floatString(0.0))
        assertEquals("1", CalcMath.floatString(1.0))
        assertEquals("-1", CalcMath.floatString(-1.0))
        assertEquals("1.5", CalcMath.floatString(1.5))
        assertEquals("0.0009765625", CalcMath.floatString(twoto(-10)))
        assertEquals("0.9990234375", CalcMath.floatString(1 - twoto(-10)))
        assertEquals("3.141592653589793", CalcMath.floatString(Math.PI))
        assertEquals("1024", CalcMath.floatString(twoto(10)))
        assertEquals("1000000", CalcMath.floatString(1e+6))
        assertEquals("1048576", CalcMath.floatString(twoto(20)))
        assertEquals("1073741824", CalcMath.floatString(twoto(30)))
        assertEquals("1099511627776", CalcMath.floatString(twoto(40)))
        assertEquals("1125899906842624", CalcMath.floatString(twoto(50)))
        // assertEquals("1152921504606846976", CalcMath.floatString(twoto(60))))
        assertEquals("1.15292150460684698E18", CalcMath.floatString(twoto(60)))
        assertEquals("1.1805916207174113E21", CalcMath.floatString(twoto(70)))
        assertEquals("0.000001", CalcMath.floatString(1e-6))
        assertEquals("0.0009765625", CalcMath.floatString(twoto(-10)))
        assertEquals("0.9990234375", CalcMath.floatString(1 - twoto(-10)))
        assertEquals("9.5367431640625E-7", CalcMath.floatString(twoto(-20)))
        assertEquals("9.313225746154785E-10", CalcMath.floatString(twoto(-30)))
        assertEquals("9.094947017729282E-13", CalcMath.floatString(twoto(-40)))
        assertEquals("8.881784197001252E-16", CalcMath.floatString(twoto(-50)))
        assertEquals("8.673617379884035E-19", CalcMath.floatString(twoto(-60)))
        assertEquals("8.470329472543003E-22", CalcMath.floatString(twoto(-70)))
    }
}
