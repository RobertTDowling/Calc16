package com.rtdti.calc16

import org.junit.Assert.*

import org.junit.Test

class NumberParseTest {
    fun twoto(e: Int) = Math.pow(2.0, e.toDouble())

    @Test
    fun parseFloat() {
        val format = NumberFormat.FLOAT
        val EPSILON = 1e-24
        val EXCEPTION = NumberFormatException::class.java
        val formatter = Formatter(CalcViewModel.FormatState(EPSILON, 0, format), 0)
        assertEquals(0.0, formatter.parser("0"), EPSILON)
        assertEquals(0.0, formatter.parser("000"), EPSILON)
        assertEquals(0.0, formatter.parser("0.0"), EPSILON)
        assertEquals(0.0, formatter.parser("0."), EPSILON)
        assertEquals(0.0, formatter.parser(".0"), EPSILON)
        assertEquals(1.0, formatter.parser("1"), EPSILON)
        assertThrows(EXCEPTION) { formatter.parser(".") }
        assertEquals(1.5, formatter.parser("1.5"), EPSILON)
        assertThrows(EXCEPTION) { formatter.parser("1.2.3") }
        assertEquals(0.1, formatter.parser(".1"), EPSILON)
        assertEquals(0.1, formatter.parser("0.1"), EPSILON)
        assertEquals(0.01, formatter.parser("0.01"), EPSILON)
        assertEquals(0.00001, formatter.parser("0.00001"), EPSILON)
        val t2a = "0.0000000000000000000001"
        val t2b = 0.0000000000000000000001
        val t2c = 1e-22
        assertEquals(t2b, formatter.parser(t2a), t2b * EPSILON)
        assertEquals(t2c, formatter.parser(t2a), t2b * EPSILON)
        assertEquals(12345.0, formatter.parser("12345"), EPSILON)
        assertEquals(1234567890.0, formatter.parser("1234567890"), EPSILON)
        assertEquals(12345678901234567890.0, formatter.parser("12345678901234567890"), EPSILON)
        val t1a = 12345678901234567890.0
        val t1b = 1234567890.0
        val t1c = t1a / t1b
        val t1d = formatter.parser("12345678901234567890")
        val t1e = t1d / t1b
        assertEquals(t1c, t1e, EPSILON)
    }

    @Test
    fun parseHex() {
        val format = NumberFormat.HEX
        val EPSILON = 1e-24
        val EXCEPTION = NumberFormatException::class.java
        val formatter = Formatter(CalcViewModel.FormatState(EPSILON, 0, format), 0)
        assertEquals(0.0, formatter.parser("0"), EPSILON)
        assertEquals(16.0, formatter.parser("10"), EPSILON)
        assertThrows(EXCEPTION) { formatter.parser("0x10") }
        assertThrows(EXCEPTION) { formatter.parser("3.75") }
        assertEquals(256 + 2 * 16 + 3.0, formatter.parser("123"), EPSILON)
        assertEquals(15.0, formatter.parser("f"), EPSILON)
        assertEquals(twoto(32) - 1, formatter.parser("ffffffff"), EPSILON)
        assertEquals(twoto(63) - 1, formatter.parser("7fffffffffffffff"), EPSILON)
        assertEquals(twoto(63), formatter.parser("7fffffffffffffff"), EPSILON)
        assertEquals(twoto(59), formatter.parser("800000000000000"), EPSILON)
        //assertEquals(twoto(63), formatter.parser("8000000000000000"), EPSILON)
        assertEquals(-twoto(63), formatter.parser("8000000000000000"), EPSILON)
        //assertEquals(twoto(64), formatter.parser("ffffffffffffffff"), EPSILON)
        assertEquals(-1.0, formatter.parser("ffffffffffffffff"), EPSILON)
    }

    fun parseFloatLite(format: NumberFormat) {
        val EPSILON = 1e-24
        val formatter = Formatter(CalcViewModel.FormatState(EPSILON, 0, format), 0)
        assertEquals(0.0, formatter.parser("0"), EPSILON)
        assertEquals(1.0, formatter.parser("1"), EPSILON)
        assertEquals(1.5, formatter.parser("1.5"), EPSILON)
        assertEquals(0.00001, formatter.parser("0.00001"), EPSILON)
    }

    @Test fun parseSci() = parseFloatLite(NumberFormat.SCI)
    @Test fun parseFix() = parseFloatLite(NumberFormat.FIX)
    @Test fun parseImproper() = parseFloatLite(NumberFormat.IMPROPER)
    @Test fun parseMixImperial() = parseFloatLite(NumberFormat.MIXIMPERIAL)
    @Test fun parsePrime() = parseFloatLite(NumberFormat.PRIME)
}