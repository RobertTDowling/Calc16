package com.rtdti.calc16

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*

import org.junit.Test

class NumberFormatTest {
    fun twoto(e: Int) = Math.pow(2.0, e.toDouble())

    @Test
    fun formatterFloat() {
        val format = NumberFormat.FLOAT
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = Formatter(formatState, 0)
        assertEquals(AnnotatedString("0"), formatter.format(0.0))
        assertEquals(AnnotatedString("-3.141592653589793"), formatter.format(-Math.PI))
        assertEquals(AnnotatedString("1000000"), formatter.format(1e+6))
        assertEquals(AnnotatedString("0.000001"), formatter.format(1e-6))
    }

    @Test
    fun formatterHex() {
        val format = NumberFormat.HEX
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = Formatter(formatState, 0)
        assertEquals(AnnotatedString("0 = 0x0"), formatter.format(0.0))
        assertEquals(AnnotatedString("1 = 0x1"), formatter.format(1.0))
        assertEquals(AnnotatedString("-1 = 0xffffffffffffffff"), formatter.format(-1.0))
        assertEquals(AnnotatedString("1.5 = 0x1 + ϵ"), formatter.format(1.5))
        assertEquals(AnnotatedString("16 = 0x10"), formatter.format(16.0))
        assertEquals(AnnotatedString("-16 = 0xfffffffffffffff0"), formatter.format(-16.0))
    }

    @Test
    fun formatterImproper() {
        val format = NumberFormat.IMPROPER
        val formatState = CalcViewModel.FormatState(1e-5, 2, format)
        val formatter = Formatter(formatState, 0)
        assertEquals(AnnotatedString("0"), formatter.format(0.0))
        assertEquals(AnnotatedString("-1"), formatter.format(-1.0))
        assertEquals(AnnotatedString("-3 / 2"), formatter.format(-3/2.0))
        assertEquals(AnnotatedString("355 / 113 + ϵ"), formatter.format(Math.PI))
    }

    @Test
    fun formatterMixImperial() {
        val format = NumberFormat.MIXIMPERIAL
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = Formatter(formatState, 0)
        assertEquals(AnnotatedString("0"), formatter.format(0.0))
        assertEquals(AnnotatedString("-1"), formatter.format(-1.0))
        assertEquals(AnnotatedString("-1 - 1 / 2"), formatter.format(-3/2.0))
        assertEquals(AnnotatedString("3 - 145 / 1024 + ϵ"), formatter.format(Math.PI))
        assertEquals(AnnotatedString("5215 / 16384 + ϵ"), formatter.format(1/Math.PI))
    }

    @Test
    fun formatterFix() {
        val format = NumberFormat.FIX
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = Formatter(formatState, 0)
        assertEquals(AnnotatedString("0.00"), formatter.format(0.0))
        assertEquals(AnnotatedString("1.00"), formatter.format(1.0))
        assertEquals(AnnotatedString("-1.50"), formatter.format(-3/2.0))
        assertEquals(AnnotatedString("3.14"), formatter.format(Math.PI))
        assertEquals(AnnotatedString("0.32"), formatter.format(1/Math.PI))
    }

    @Test
    fun formatterSci() {
        val format = NumberFormat.SCI
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = Formatter(formatState, 0)
        assertEquals(AnnotatedString("0.00e+00"), formatter.format(0.0))
        assertEquals(AnnotatedString("1.00e+00"), formatter.format(1.0))
        assertEquals(AnnotatedString("-1.50e+00"), formatter.format(-3/2.0))
        assertEquals(AnnotatedString("3.14e+00"), formatter.format(Math.PI))
        assertEquals(AnnotatedString("3.18e-01"), formatter.format(1/Math.PI))
    }

    @Test
    fun formatterPrime() {
        val format = NumberFormat.PRIME
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = Formatter(formatState, 0)
        assertEquals(AnnotatedString("0 = 0"), formatter.format(0.0))
        assertEquals(AnnotatedString("1 = 1"), formatter.format(1.0))
        assertEquals(AnnotatedString("1.5 = 1"), formatter.format(3/2.0))
        assertEquals(AnnotatedString("-1 = -1"), formatter.format(-1.0))
        assertEquals(AnnotatedString("-6 = -2*3"), formatter.format(-6.0))
        assertEquals(AnnotatedString("2310 = 2*3*5*7*11"), formatter.format(2*3*5*7*11.0))
        assertEquals(AnnotatedString("9 = 3^2"), formatter.format(9.0))
        assertEquals(AnnotatedString("123456789 = 3^2*3607*3803"), formatter.format(123456789.0))
    }

    @Test
    fun formatterTime() {
        val format = NumberFormat.TIME
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = Formatter(formatState, 0)
        assertEquals(AnnotatedString("0 = 0:00"), formatter.format(0.0))
        assertEquals(AnnotatedString("1.5 = 1:30"), formatter.format(1.5))
        assertEquals(AnnotatedString("-1.25 = -1:15"), formatter.format(-1.25))
    }
}
