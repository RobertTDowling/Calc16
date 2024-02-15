package com.rtdti.calc16

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*

import org.junit.Test

class NumberFormatTest {
    fun twoto(e: Double) = Math.pow(2.0,e)
    @Test
    fun formatterFloat() {
        val format = NumberFormat.FLOAT
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = format.formatter()
        assertEquals(AnnotatedString("0"), formatter.format(0.0, formatState))
        assertEquals(AnnotatedString("1"), formatter.format(1.0, formatState))
        assertEquals(AnnotatedString("-1"), formatter.format(-1.0, formatState))
        assertEquals(AnnotatedString("1.5"), formatter.format(1.5, formatState))
        assertEquals(AnnotatedString("0.0009765625"), formatter.format(twoto(-10.0), formatState))
        assertEquals(AnnotatedString("0.9990234375"), formatter.format(1-twoto(-10.0), formatState))
        assertEquals(AnnotatedString("3.141592653589793"), formatter.format(Math.PI, formatState))
        assertEquals(AnnotatedString("1024"), formatter.format(twoto(10.0), formatState))
        assertEquals(AnnotatedString("1000000"), formatter.format(1e+6, formatState))
        assertEquals(AnnotatedString("1048576"), formatter.format(twoto(20.0), formatState))
        assertEquals(AnnotatedString("1073741824"), formatter.format(twoto(30.0), formatState))
        assertEquals(AnnotatedString("1099511627776"), formatter.format(twoto(40.0), formatState))
        assertEquals(AnnotatedString("1125899906842624"), formatter.format(twoto(50.0), formatState))
        // assertEquals(AnnotatedString("1152921504606846976"), formatter.format(twoto(60.0), formatState))
        assertEquals(AnnotatedString("1.15292150460684698E18"), formatter.format(twoto(60.0), formatState))
        assertEquals(AnnotatedString("1.1805916207174113E21"), formatter.format(twoto(70.0), formatState))
        assertEquals(AnnotatedString("0.000001"), formatter.format(1e-6, formatState))
        assertEquals(AnnotatedString("0.0009765625"), formatter.format(twoto(-10.0), formatState))
        assertEquals(AnnotatedString("0.9990234375"), formatter.format(1-twoto(-10.0), formatState))
        assertEquals(AnnotatedString("9.5367431640625E-7"), formatter.format(twoto(-20.0), formatState))
        assertEquals(AnnotatedString("9.313225746154785E-10"), formatter.format(twoto(-30.0), formatState))
        assertEquals(AnnotatedString("9.094947017729282E-13"), formatter.format(twoto(-40.0), formatState))
        assertEquals(AnnotatedString("8.881784197001252E-16"), formatter.format(twoto(-50.0), formatState))
        assertEquals(AnnotatedString("8.673617379884035E-19"), formatter.format(twoto(-60.0), formatState))
        assertEquals(AnnotatedString("8.470329472543003E-22"), formatter.format(twoto(-70.0), formatState))
    }
    @Test
    fun formatterHex() {
        val format = NumberFormat.HEX
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = format.formatter()
        assertEquals(AnnotatedString("0x0"), formatter.format(0.0, formatState))
        assertEquals(AnnotatedString("0x1"), formatter.format(1.0, formatState))
        assertEquals(AnnotatedString("0xffffffffffffffff"), formatter.format(-1.0, formatState))
        assertEquals(AnnotatedString("0x1 + ϵ"), formatter.format(1.5, formatState))
        assertEquals(AnnotatedString("0x10"), formatter.format(16.0, formatState))
        assertEquals(AnnotatedString("0xfffffffffffffff0"), formatter.format(-16.0, formatState))
    }
    @Test
    fun formatterImproper() {
        val format = NumberFormat.IMPROPER
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = format.formatter()
        assertEquals(AnnotatedString("0"), formatter.format(0.0, formatState))
        assertEquals(AnnotatedString("-1"), formatter.format(-1.0, formatState))
        assertEquals(AnnotatedString("-3 / 2"), formatter.format(-3/2.0, formatState))
        assertEquals(AnnotatedString("355 / 113 + ϵ"), formatter.format(Math.PI, formatState))
    }
    @Test
    fun formatterMixImperial() {
        val format = NumberFormat.MIXIMPERIAL
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = format.formatter()
        assertEquals(AnnotatedString("0"), formatter.format(0.0, formatState))
        assertEquals(AnnotatedString("-1"), formatter.format(-1.0, formatState))
        assertEquals(AnnotatedString("-1 - 1 / 2"), formatter.format(-3/2.0, formatState))
        assertEquals(AnnotatedString("3 - 145 / 1024 + ϵ"), formatter.format(Math.PI, formatState))
        assertEquals(AnnotatedString("5215 / 16384 + ϵ"), formatter.format(1/Math.PI, formatState))
    }
    @Test
    fun formatterFix() {
        val format = NumberFormat.FIX
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = format.formatter()
        assertEquals(AnnotatedString("0.00"), formatter.format(0.0, formatState))
        assertEquals(AnnotatedString("1.00"), formatter.format(1.0, formatState))
        assertEquals(AnnotatedString("-1.50"), formatter.format(-3/2.0, formatState))
        assertEquals(AnnotatedString("3.14"), formatter.format(Math.PI, formatState))
        assertEquals(AnnotatedString("0.32"), formatter.format(1/Math.PI, formatState))
    }
    @Test
    fun formatterSci() {
        val format = NumberFormat.SCI
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = format.formatter()
        assertEquals(AnnotatedString("0.00e+00"), formatter.format(0.0, formatState))
        assertEquals(AnnotatedString("1.00e+00"), formatter.format(1.0, formatState))
        assertEquals(AnnotatedString("-1.50e+00"), formatter.format(-3/2.0, formatState))
        assertEquals(AnnotatedString("3.14e+00"), formatter.format(Math.PI, formatState))
        assertEquals(AnnotatedString("3.18e-01"), formatter.format(1/Math.PI, formatState))
    }
    @Test
    fun formatterPrime() {
        val format = NumberFormat.PRIME
        val formatState = CalcViewModel.FormatState(1e-4, 2, format)
        val formatter = format.formatter()
        assertEquals(AnnotatedString("0 = 0"), formatter.format(0.0, formatState))
        assertEquals(AnnotatedString("1 = 1"), formatter.format(1.0, formatState))
        assertEquals(AnnotatedString("1 = 1"), formatter.format(3/2.0, formatState))
        assertEquals(AnnotatedString("-1 = -1"), formatter.format(-1.0, formatState))
        assertEquals(AnnotatedString("-6 = -2·3"), formatter.format(-6.0, formatState))
        assertEquals(AnnotatedString("2310 = 2·3·5·7·11"), formatter.format(2*3*5*7*11.0, formatState))
        assertEquals(AnnotatedString("9 = 3^2"), formatter.format(9.0, formatState))
        assertEquals(AnnotatedString("123456789 = 3^2·3607·3803"), formatter.format(123456789.0, formatState))
    }
}