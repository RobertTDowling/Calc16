package com.rtdti.calc16

import org.junit.Assert.*

import org.junit.Test
import kotlin.random.Random

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
    fun mydouble2frac() {
        assertEquals(Frac(11L, 4L, 0.0), CalcMath.mydouble2frac(2.75, EPSILON))
        assertEquals(Frac(0L, 1L, 0.0), CalcMath.mydouble2frac(0.0, EPSILON))
        assertEquals(Frac(1L, 1L, 0.0), CalcMath.mydouble2frac(1.0, EPSILON))
        assertEquals(Frac(-2L, 1L, 0.0), CalcMath.mydouble2frac(-2.0, EPSILON))
        assertEquals(Frac(22L, 7L, 0.0), CalcMath.mydouble2frac(22/7.0, EPSILON))
        assertTrue(Frac(22L, 7L, 0.0).near(CalcMath.mydouble2frac(22/7.0, EPSILON), EPSILON))
        assertNotEquals(Frac(355L, 113L, 0.0), CalcMath.mydouble2frac(Math.PI, EPSILON))
        assertTrue(Frac(355L, 113L, 0.0).near(CalcMath.mydouble2frac(Math.PI, EPSILON/10), EPSILON/10))
        assertTrue(Frac(3L, 1L, 0.141592654).near(CalcMath.mydouble2frac(Math.PI, 1e-0), 1e-0))
        assertTrue(Frac(22L, 7L, 0.0013).near(CalcMath.mydouble2frac(Math.PI, 1e-2), 1e-2))
        assertTrue(Frac(333L, 106L, 9e-5).near(CalcMath.mydouble2frac(Math.PI, 1e-4), 1e-4))
        assertTrue(Frac(355L, 113L, 3e-7).near(CalcMath.mydouble2frac(Math.PI, 1e-6), 1e-6))

        assertTrue(Frac(2L, 1L, 0.7182818).near(CalcMath.mydouble2frac(Math.E, 1e-0), 1e-0))
        assertTrue(Frac(8L, 3L, 6e-2).near(CalcMath.mydouble2frac(Math.E, 1e-1), 1e-1))
        assertTrue(Frac(19L, 7L, 4e-3).near(CalcMath.mydouble2frac(Math.E, 1e-2), 1e-2))
        assertTrue(Frac(87L, 32L, 5e-4).near(CalcMath.mydouble2frac(Math.E, 1e-3), 1e-3))
        assertTrue(Frac(193L, 71L, 3e-5).near(CalcMath.mydouble2frac(Math.E, 1e-4), 1e-4))
        assertTrue(Frac(1264L, 465L, 3e-6).near(CalcMath.mydouble2frac(Math.E, 1e-5), 1e-5))
        assertTrue(Frac(2721L, 1001L, 2e-7).near(CalcMath.mydouble2frac(Math.E, 1e-6), 1e-6))
        val PHI = (1+Math.sqrt(5.0))/2
        assertTrue(Frac(1L, 1L, 0.618033988).near(CalcMath.mydouble2frac(PHI, 1e-0), 1e-0))
        assertTrue(Frac(5L, 3L, 5e-2).near(CalcMath.mydouble2frac(PHI, 1e-1), 1e-1))
        assertTrue(Frac(13L, 8L, 7e-3).near(CalcMath.mydouble2frac(PHI, 1e-2), 1e-2))
        assertTrue(Frac(55L, 34L, 4e-4).near(CalcMath.mydouble2frac(PHI, 1e-3), 1e-3))
        assertTrue(Frac(144L, 89L, 6e-5).near(CalcMath.mydouble2frac(PHI, 1e-4), 1e-4))
        assertTrue(Frac(377L, 233L, 9e-6).near(CalcMath.mydouble2frac(PHI, 1e-5), 1e-5))
        assertTrue(Frac(1597L, 987L, 5e-7).near(CalcMath.mydouble2frac(PHI, 1e-6), 1e-6))
    }

    @Test
    fun mydouble2fracNeg() {
        // Crazy epsilon values
        assertTrue(Frac(245850922L, 78256779L, 0.0).near(CalcMath.mydouble2frac(Math.PI, 0.0), EPSILON))
        assertTrue(Frac(3L, 1L, -1.0).near(CalcMath.mydouble2frac(Math.PI, -1.0), -1.0))
        assertTrue(Frac(3141592653589L, 1L, 0.0).near(CalcMath.mydouble2frac(Math.PI*1e12, 1.0), 1.0))
        assertTrue(Frac(4L, 1273239544735L, 0.0).near(CalcMath.mydouble2frac(Math.PI*1e-12, 1e-24), 1e-24))
        // Negative inputs (which sometimes cause sign to appear on denom instead of num)
        assertTrue(Frac(-333L, 106L, 0.0).near(CalcMath.mydouble2frac(-Math.PI, EPSILON), EPSILON))
        assertTrue(Frac(-355L, 113L, 0.0).near(CalcMath.mydouble2frac(-Math.PI, EPSILON/10), EPSILON/10))
        // Detectable error conditions
        assertEquals(Frac(1000000000000000000L, 1L, 0.0), CalcMath.mydouble2frac(1e18, 0.0))
        assertEquals(Frac(1L, 100000000000000000L, 0.0), CalcMath.mydouble2frac(1e-17, 0.0))
        assertEquals(Frac(0L, 1L, 1e-18), CalcMath.mydouble2frac(1e-18, 1e-18))
        assertEquals(Frac(0L, 0L, 0.0), CalcMath.mydouble2frac(1e19, 0.0))
        assertEquals(Frac(0L, 0L, 0.0), CalcMath.mydouble2frac(1e-19, 0.0))
        assertEquals(Frac(0L, 0L, 0.0), CalcMath.mydouble2frac(1e104, 0.0))
        assertEquals(Frac(0L, 0L, 0.0), CalcMath.mydouble2frac(1e-104, 0.0))
        assertEquals(Frac(0L, 1L, 0.0), CalcMath.mydouble2frac(0.0, 0.0))
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
        // assertEquals("641*6700417*2147483647", primeFactorString((twoto(31)-1) * (twoto(32)+1))) // too slow!
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

    @Test
    fun negative_sqrt_etc() {
        assertEquals(2.0, CalcMath.sqrt(4.0), EPSILON)
        assertTrue(CalcMath.sqrt(-4.0).isInfinite())
        assertEquals(0.0, CalcMath.ln(1.0), EPSILON)
        assertTrue(CalcMath.ln(-4.0).isInfinite())
        assertEquals(2.0, CalcMath.log10(100.0), EPSILON)
        assertTrue(CalcMath.log10(-4.0).isInfinite())
        assertEquals(2.0, CalcMath.log2(4.0), EPSILON)
        assertTrue(CalcMath.log2(-4.0).isInfinite())
    }
    @Test
    fun signCrop() {
        assertEquals(0x5.toDouble(), CalcMath.signCrop(0x5.toDouble()), EPSILON)
        assertEquals(0x5.toDouble(), CalcMath.signCrop(0xd.toDouble()), EPSILON)
        assertEquals(0x5.toDouble(), CalcMath.signCrop(0x1d.toDouble()), EPSILON)
        assertEquals(0x5.toDouble(), CalcMath.signCrop(0xfd.toDouble()), EPSILON)
        assertEquals(0x5.toDouble(), CalcMath.signCrop(-0x3.toDouble()), EPSILON) // F...Fd

        assertEquals(0x0.toDouble(), CalcMath.signCrop(0x0.toDouble()), EPSILON)
        assertEquals(0x1.toDouble(), CalcMath.signCrop(0x1.toDouble()), EPSILON)
        assertEquals(0x2.toDouble(), CalcMath.signCrop(0x2.toDouble()), EPSILON)
        assertEquals(0x1.toDouble(), CalcMath.signCrop(0x3.toDouble()), EPSILON)
        assertEquals(0x4.toDouble(), CalcMath.signCrop(0x4.toDouble()), EPSILON)
        assertEquals(0x5.toDouble(), CalcMath.signCrop(0x5.toDouble()), EPSILON)
        assertEquals(0x2.toDouble(), CalcMath.signCrop(0x6.toDouble()), EPSILON)
        assertEquals(0x1.toDouble(), CalcMath.signCrop(0x7.toDouble()), EPSILON)
        assertEquals(0x8.toDouble(), CalcMath.signCrop(0x8.toDouble()), EPSILON)
        assertEquals(0x9.toDouble(), CalcMath.signCrop(0x9.toDouble()), EPSILON)
        assertEquals(0xa.toDouble(), CalcMath.signCrop(0xa.toDouble()), EPSILON)
        assertEquals(0xb.toDouble(), CalcMath.signCrop(0xb.toDouble()), EPSILON)
        assertEquals(0x4.toDouble(), CalcMath.signCrop(0xc.toDouble()), EPSILON)
        assertEquals(0x5.toDouble(), CalcMath.signCrop(0xd.toDouble()), EPSILON)
        assertEquals(0x2.toDouble(), CalcMath.signCrop(0xe.toDouble()), EPSILON)
        assertEquals(0x1.toDouble(), CalcMath.signCrop(0xf.toDouble()), EPSILON)
        assertEquals(0x10.toDouble(), CalcMath.signCrop(0x10.toDouble()), EPSILON)
        assertEquals(0x1234.toDouble(), CalcMath.signCrop(0x1234.toDouble()), EPSILON)
        assertEquals(0x1456.toDouble(), CalcMath.signCrop(0x3456.toDouble()), EPSILON)
        assertEquals(0xa56.toDouble(), CalcMath.signCrop(0x1a56.toDouble()), EPSILON)
        assertEquals(0x1.toDouble(), CalcMath.signCrop(-0x1.toDouble()), EPSILON) // ff
        assertEquals(0x2.toDouble(), CalcMath.signCrop(-0x2.toDouble()), EPSILON) // fe
        assertEquals(0x5.toDouble(), CalcMath.signCrop(-0x3.toDouble()), EPSILON) // fd
        assertEquals(0x4.toDouble(), CalcMath.signCrop(-0x4.toDouble()), EPSILON) // fc
        assertEquals(0xb.toDouble(), CalcMath.signCrop(-0x5.toDouble()), EPSILON) // fb
        assertEquals(0xa.toDouble(), CalcMath.signCrop(-0x6.toDouble()), EPSILON) // fa
        assertEquals(0x9.toDouble(), CalcMath.signCrop(-0x7.toDouble()), EPSILON) // f9
        assertEquals(0x8.toDouble(), CalcMath.signCrop(-0x8.toDouble()), EPSILON) // f8
        assertEquals(0x17.toDouble(), CalcMath.signCrop(-0x9.toDouble()), EPSILON) // f7
        assertEquals(0x16.toDouble(), CalcMath.signCrop(-0xa.toDouble()), EPSILON) // f6
        assertEquals(0x15.toDouble(), CalcMath.signCrop(-0xb.toDouble()), EPSILON) // f5
        assertEquals(0x14.toDouble(), CalcMath.signCrop(-0xc.toDouble()), EPSILON) // f4
        assertEquals(0x13.toDouble(), CalcMath.signCrop(-0xd.toDouble()), EPSILON) // f3
        assertEquals(0x12.toDouble(), CalcMath.signCrop(-0xe.toDouble()), EPSILON) // f2
        assertEquals(0x11.toDouble(), CalcMath.signCrop(-0xf.toDouble()), EPSILON) // f1
        assertEquals(0x10.toDouble(), CalcMath.signCrop(-0x10.toDouble()), EPSILON) // f0
    }
    @Test
    fun signExtend() {
        assertEquals(-0x3.toDouble(), CalcMath.signExtend(0x5.toDouble()), EPSILON)
        assertEquals(-0x3.toDouble(), CalcMath.signExtend(0xd.toDouble()), EPSILON)
        assertEquals(-0x3.toDouble(), CalcMath.signExtend(0x1d.toDouble()), EPSILON)
        assertEquals(-0x3.toDouble(), CalcMath.signExtend(0xfd.toDouble()), EPSILON)
        assertEquals(-0x3.toDouble(), CalcMath.signExtend(-0x3.toDouble()), EPSILON) // F...Fd

        assertEquals(-0x0.toDouble(), CalcMath.signExtend(0x0.toDouble()), EPSILON)
        assertEquals(-0x1.toDouble(), CalcMath.signExtend(0x1.toDouble()), EPSILON)
        assertEquals(-0x2.toDouble(), CalcMath.signExtend(0x2.toDouble()), EPSILON)
        assertEquals(-0x1.toDouble(), CalcMath.signExtend(0x3.toDouble()), EPSILON)
        assertEquals(-0x4.toDouble(), CalcMath.signExtend(0x4.toDouble()), EPSILON)
        assertEquals(-0x8.toDouble(), CalcMath.signExtend(0x8.toDouble()), EPSILON)
        assertEquals(-0x7.toDouble(), CalcMath.signExtend(0x9.toDouble()), EPSILON)
        assertEquals(-0x6.toDouble(), CalcMath.signExtend(0xa.toDouble()), EPSILON)
        assertEquals(-0x5.toDouble(), CalcMath.signExtend(0xb.toDouble()), EPSILON)
        assertEquals(-0x4.toDouble(), CalcMath.signExtend(0xc.toDouble()), EPSILON)
        assertEquals(-0xa.toDouble(), CalcMath.signExtend(0x76.toDouble()), EPSILON)
        assertEquals(-0x66.toDouble(), CalcMath.signExtend(0x9a.toDouble()), EPSILON)
        assertEquals(-0x09ac.toDouble(), CalcMath.signExtend(0x7654.toDouble()), EPSILON) // wow, I don't see this
        assertEquals(-0x6544.toDouble(), CalcMath.signExtend(0x9abc.toDouble()), EPSILON)
        assertEquals(-0x9abcdf0.toDouble(), CalcMath.signExtend(0x76543210.toDouble()), EPSILON)
        assertEquals(-(0x65432110.toDouble()), CalcMath.signExtend(0x9abcdef0.toDouble()), EPSILON)
    }
    @Test
    fun log2() {
        fun log2l(v: Long): Int { // Should be same as ffo
            for (i in 63 downTo 0) {
                if (v.and(1L.shl(i)) != 0L) {
                    return i
                }
            }
            return -1
        }
        for (i in 1L..1000000L) {
            assertEquals(CalcMath.log2(i.toDouble()).toInt(), CalcMath.ffo(i))
            assertEquals(log2l(i), CalcMath.ffo(i))
        }
        // Randori attack
        val r = Random.Default
        for (j in 0..1000000) {
            val i = r.nextLong()
            if (i <= 0L)
                continue
            assertEquals(CalcMath.log2(i.toDouble()).toInt(), CalcMath.ffo(i))
            assertEquals(log2l(i), CalcMath.ffo(i))
        }
        val v2 = -3L // fff...fd
        val v3 = CalcMath.ffo(v2)
        assertEquals(63, v3)
    }
}
