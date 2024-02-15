package com.rtdti.calc16

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

data class Frac(val num: Long, val denom: Long, val err: Double)

object CalcMath {
    fun double2frac (startx: Double, epsilon: Double) : Frac {
        // Taken from float_to_frac.c
        //** find rational approximation to given real number
        //** David Eppstein / UC Irvine / 8 Aug 1993
        //**
        //** With corrections from Arno Formella, May 2008

        var x = startx
        val maxden = 1/epsilon;

        var m00 = 1L
        var m11 = 1L
        var m10 = 0L
        var m01 = 0L

        var loops = 0
        var ai = x.toLong()

        // loop finding terms until denom gets too big
        while (m10 * ai + m11 <= maxden && loops < 100) {
            loops++;
            val t1 = m00 * ai + m01;
            m01 = m00;
            m00 = t1;
            val t2 = m10 * ai + m11;
            m11 = m10;
            m10 = t2;
            if (x==ai.toDouble()) {
                break     // AF: division by zero
            }
            x = 1/(x - ai.toDouble())
            if (x>0x7FFFFFFF) {
                break     // AF: representation failure
            }
            ai = x.toLong()
        }
        /* now remaining x is between 0 and 1/ai */
        /* approx as either 0 or 1/m where m is max that will fit in maxden */
        /* first try zero */
        val n1 = m00
        val d1 = m10
        val e1 = startx - m00.toDouble() / m10
        /* now try other possibility */
        ai = ((maxden - m11) / m10).toLong();
        m00 = m00 * ai + m01;
        m10 = m10 * ai + m11;

        val n2 = m00
        val d2 = m10;
        val e2 = startx - m00.toDouble() / m10

        if (e1.absoluteValue < e2.absoluteValue) {
            // Pick e1
            return Frac(n1, d1, e1)
        } else {
            // Pick e2
            return Frac(n2, d2, e2)
        }
    }

    fun gcd(a: Long, b: Long) : Long {
        var a=a
        var b=b
        while (a>0 && b>0) {
            if (a < b) {
                a = b.also({ b = a })
            }
            a %= b
        }
        return if (b < 1) 1L else b
    }

    fun lcm(a: Long, b: Long) : Long {
        return a*b/gcd(a,b)
    }

    fun double2imperial(x0: Double, epsilon: Double): Frac {
        val x = x0.absoluteValue
        val sign = if (x0 < 0) -1 else 1
        val invEpsilon2 = Math.pow(2.0, Math.ceil(Math.log(1 / epsilon) / Math.log(2.0)))
        var n: Long = (x * invEpsilon2).roundToLong()
        var d: Long = invEpsilon2.roundToLong()
        // if (d < 0) d = -d
        if (d < 1) {
            // avoid trouble with numbers we can't convert
            return Frac(x0.roundToLong(), 1L, x0-x0.roundToLong())
        }
        val g: Long = gcd(n, d)
        n /= g
        d /= g
        if (n == 0L) d = g // fix d=1
        return Frac(sign * n, d, x-n.toDouble()/d)
    }

    fun primeFactorAnnotatedString(x: Long, fs: Int): AnnotatedString {
        val testable = fs == 0 // Unit tests don't set fs, so draw a ^ instead of superscript
        return buildAnnotatedString {
            fun renderFactor(b: Long, p: Int, cdot: Boolean) {
                if (cdot) {
                    append("·")
                }
                append(b.toString())
                if (p>1) {
                    if (testable) {
                        append("^")
                        append(p.toString())
                    } else {
                        withStyle(
                            style = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = fs.sp)
                        ) { append(p.toString()) }
                    }
                }
            }
            var any = false
            val sign = x<0
            val end = Math.round(Math.sqrt(x.toDouble().absoluteValue));
            var a = x.absoluteValue
            var b = 2L
            var p = 0
            if (sign) { append("-") }
            append(a.toString() + " = ")
            if (sign) { append("-") }
            if (a < 2) {
                append(a.toString())
                any = true
            }
            while (a > 1) {
                if ((a % b) == 0L) {
                    p++
                    a /= b
                } else {
                    if (p > 0) {
                        renderFactor(b, p, any)
                        any = true
                        p = 0
                    }
                    b += if (b == 2L) 1L else 2L
                    if (b > end) {
                        p++
                        b = a
                        break
                    }
                }
            }
            if (p > 0) {
                renderFactor(b, p, any)
            }
        } // buildAnnotatedString
    }
}