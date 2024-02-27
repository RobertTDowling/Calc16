package com.rtdti.calc16

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToLong

data class Frac(val num: Long, val denom: Long, val err: Double) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Frac) return false
        if (num != other.num) return false
        if (denom != other.denom) return false
        if (err != other.err) return false
        return true
    }
    fun near(other: Frac, epsilon: Double): Boolean {
        if (num != other.num) return false
        if (denom != other.denom) return false
        return maxOf(err.absoluteValue, other.err.absoluteValue) <= epsilon.absoluteValue
    }
}
object CalcMath {
    /*
Pi to 5 decimal places is 355/113. How did we find that? Continued Fractions.

x = a₀ + 1
        ------
        a₁ + 1
            ------
            a₂ + 1
                ------
                a₃ + ...

Using our example x = pi,

Let a₀ = floor(x) = 3 (N.B. floor(x) is the largest integer less than x)
Then a₁ = floor (1/(pi-a₀)) = floor(1/0.14159..) = floor (7.06151...) = 7
Then a₂ = floor (1/(7.06151..-7)) = floor (15.99659..) = 15
Then a₃ = floor (1/(15.99659..-15)) = floor (1.003417...) = 1
etc.

In other words

pi = 3 + 1
        ------
         7 + 1
            ------
            15 + 1
                ------
                1 + ...


At any step along the way, we can truncate the continued fraction by
introducing an error term εᵢ:


x = a₀ + ε₀, for i=0

            1
x = a₀ + -------, for i=1
         a₁ + ε₁


            1
x = a₀ + ----------, for i=2
                 1
         a₁ + -------
              a₂ + ε₂

We can solve for the i'th error by noting in the sequence above:


        1
ε₀ = -------
     a₁ + ε₁

        1
ε₁ = -------
     a₂ + ε₂

          1
εᵢ = -----------
     aᵢ₊ᵢ + εᵢ₊ᵢ


Setting εᵢ = 0, we can extract an approximate fraction for x:


For i=1,

            1    |      a₀a₁ + 1     22   n₁=22
x = a₀ + ------- |   ≅  --------  =  --
         a₁ + ε₁ |         a₁        7    d₁=7
                 | ε₁=0


for i=2,

            1         |      a₀a₁a₂ + a₀ + a₂     333    n₂=333
x = a₀ + -------      |   ≅  ----------------  =  ---
         a₁ +  1      |          a₁a₂ + 1         106    d₂=106
              ------- |
              a₂ + ε₂ |
                      | ε₂=0

for i=3

            1               |      a₀a₁a₂a₃ + a₀a₁ + a₀a₃ + a₂a₃ + 1     355
x = a₀ + -------            |   ≅  ---------------------------------  =  ---
         a₁ +  1            |              a₁a₂a₃ + a₁ + a₃              113
              -------       |
              a₂ +  1       |
                   -------  |             n₃=355, d₃=113
                   a₃ + ε₃  |
                            | ε₃=0


Noted for each step i are the numerator (nᵢ) and denominator (dᵢ) of the
approximate fraction for that step.

Let n₀=3 and d₀=1, the first rational approximation 3/1, and n₁=22 and d₁=7 as
noted above.

By observation, we can see that

n₂ = n₁a₂ + n₀
d₂ = d₁a₂ + d₀

n₃ = n₂a₃ + n₁
d₃ = d₂a₃ + d₁

We can "solve backwards" for n₋₁ and d₋₁:

n₁ = n₀a₁ + n₋₁ = a₀a₁ + 1    => n₋₁ = 1

d₁ = d₀a₁ + d₋₁ = a₁          => d₋₁ = 0

While having the -1'th approximation be 1/0 is nonsensical, having these
initial values lets us define successive terms recursively:

   for i≥1

      nᵢ = nᵢ₋₁ * aᵢ + nᵢ₋₂
      dᵢ = dᵢ₋₁ * aᵢ + dᵢ₋₂
      bᵢ = 1/(bᵢ₋₁ - aᵢ₋₁))
      aᵢ = floor(bᵢ)

   Where

      n₋₁ = 1
      d₋₁ = 0
      b₀ = x
      a₀ = n₀ = floor(b₀)
      d₀ = 1

     */
    fun mydouble2frac (x: Double, epsilon: Double): Frac {
        val failureFrac = Frac(0, 0, 0.0) // Return value for impossible or non-convergent results
        val sign = if (x<0.0) -1L else 1L
        val x = x.absoluteValue
        if (x != 0.0 && (x > Long.MAX_VALUE || 1/x > Long.MAX_VALUE)) {
            return failureFrac // Give up if num or denom would be too big
        }
        val epsilon = epsilon.absoluteValue
        var n_1 = 1L
        var d_1 = 0L
        var b0 = x
        var a0 = b0.toLong()
        var n0 = a0
        var d0 = 1L

        var err = 0.0
        var count = 100
        while (count > 0) {
            err = (x - n0 / d0.toDouble())
            if (err.absoluteValue <= epsilon)
                break
            val b1 = 1 / (b0 - a0)
            val a1 = b1.toLong()
            val n1 = n0 * a1 + n_1
            val d1 = d0 * a1 + d_1
            n_1 = n0
            n0 = n1
            d_1 = d0
            d0 = d1
            a0 = a1
            b0 = b1
            count--
        }
        return if (count > 0) Frac(sign * n0, d0, err) else failureFrac
    }

    /*
    fun double2frac (startx: Double, maxden: Double) : Frac {
        // Taken from float_to_frac.c
        // find rational approximation to given real number
        // David Eppstein / UC Irvine / 8 Aug 1993
        //
        // With corrections from Arno Formella, May 2008

        var x = startx

        var m00 = 1L
        var m11 = 1L
        var m10 = 0L
        var m01 = 0L

        var loops = 0
        var ai = x.toLong()

        // loop finding terms until denom gets too big
        while (m10 * ai + m11 <= maxden && loops < 100) {
            loops++
            val t1 = m00 * ai + m01
            m01 = m00
            m00 = t1
            val t2 = m10 * ai + m11
            m11 = m10
            m10 = t2
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
        ai = ((maxden - m11) / m10).toLong()
        m00 = m00 * ai + m01
        m10 = m10 * ai + m11

        val n2 = m00
        val d2 = m10
        val e2 = startx - m00.toDouble() / m10

        return if (e1.absoluteValue < e2.absoluteValue) {
            Frac(n1, d1, e1) // Pick e1
        } else {
            Frac(n2, d2, e2) // Pick e2
        }
    }
    */
    fun gcd(a: Long, b: Long) : Long {
        if (a==0L) { return 1L }
        var aa=a.absoluteValue
        var bb=b.absoluteValue
        while (aa>0 && bb>0) {
            if (aa < bb) {
                aa = bb.also({ bb = aa }) // Swap aa,bb
            }
            aa %= bb
        }
        return if (bb < 1) 1L else bb
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
                    append(if (testable) "*" else "·")
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
            val end = Math.round(Math.sqrt(x.toDouble().absoluteValue))
            var a = x.absoluteValue
            var b = 2L
            var p = 0
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
    fun timeString(value: Double): String {
        val sign = if (value < 0.0) "-" else ""
        val hr = Math.floor(value.absoluteValue).toInt()
        val minFloat = 60 * (value.absoluteValue - hr)
        val min = Math.round(minFloat).toInt()
        return String.format("%s%d:%02d", sign, hr, min)
    }

    enum class FloatWay { NUMBER_FORMATTER, LAZY, BY_HAND }
    val Double.mantissa get() = toBits() and 0x000fffffffffffff
    val Double.exponent get() = toBits() and 0x7ff0000000000000 shr 52
    fun floatString(value: Double): String {
        val way = FloatWay.NUMBER_FORMATTER
        when(way) {
            FloatWay.BY_HAND -> {
                val mant = value.mantissa + 0x000fffffffffffff + 1
                val exp = value.exponent - 1023
                val str = String.format("%s = %x,%d", value.toString(), mant, exp)
                return str
            }

            FloatWay.NUMBER_FORMATTER -> { // Can't Unit test
                /* Android NumberFormatter
                val str = NumberFormatter.withLocale(ULocale.US)// .withLocale(Locale.US)
                    .grouping(NumberFormatter.GroupingStrategy.OFF)
                    .precision(Precision.unlimited())
                    .format(value).toString()
                 */
                lateinit var str: String
                if (value.absoluteValue > 1e16 || value.absoluteValue > 0 && value.absoluteValue < 1e-6) {
                    // Punt on big and small values (but not zero)
                    str = value.toString()
                } else {
                    val nf = DecimalFormat.getInstance()
                    nf.maximumFractionDigits = 16
                    nf.maximumIntegerDigits = 16
                    nf.isGroupingUsed = false
                    str = nf.format(value)
                }
                return str
            }

            FloatWay.LAZY -> {
                val intPart = value.toLong()
                if (intPart.toDouble() == value) { // Format as an Int if we can
                    return intPart.toString()
                }
                return value.toString()
            }
        }
    }
    fun sqrt(value: Double): Double {
        return if (value >=0) Math.sqrt(value) else Double.NEGATIVE_INFINITY
    }

    fun ln(value: Double): Double {
        return if (value >=0) Math.log(value) else Double.NEGATIVE_INFINITY
    }
    fun log10(value: Double): Double {
        return if (value >=0) Math.log10(value) else Double.NEGATIVE_INFINITY
    }
    fun log2(value: Double): Double {
        return if (value >=0) Math.log(value)/Math.log(2.0) else Double.NEGATIVE_INFINITY
    }
    fun ffo(v: Long): Int {
        if (v<0L)
            return 63
        var mask = 1L.shl(62)
        var i = 62
        while (i>=0) {
            if (v.and(mask) != 0L) {
                return i
            }
            i--
            mask=mask.shr(1)
        }
        return -1
    }
    fun signExtend(value: Double): Double {
        //return if (value > 0) value - pow2(ceil(log2(value+1))) else value // Also works
        return if (value > 0) value - pow2(1.0+ffo(value.toLong())) else value
    }
    fun signCrop(value: Double): Double {
        if (value == 0.0)
            return value
        var v = value.toLong()
        val b1 = ffo(v) // floor(log2(value)).toInt()
        var b2 = b1 - 1
        while (b2 >= 0 && v.shr(b2).and(1) == 1L) {
            b2--
        }
        return v.and(1L.shl(b2 + 2) - 1).toDouble()
    }
    fun add(a: Double, b: Double): Double { return a+b }
    fun and(a: Double, b: Double): Double { return a.toLong().and(b.toLong()).toDouble() }
    fun chs(a: Double): Double { return -a }
    fun degToRad(a: Double): Double { return Math.PI*a/180 }
    fun div(a: Double, b: Double): Double { return a/b }
    fun div2(a: Double): Double { return a/2 }
    fun inv(a: Double): Double { return 1/a }
    fun mod(a: Double, b: Double): Double { return a%b }
    fun mul(a: Double, b: Double): Double { return a*b }
    fun not(a: Double): Double { return -1.0-a }
    fun or(a: Double, b: Double): Double { return a.toLong().or(b.toLong()).toDouble() }
    fun pow10(a: Double): Double { return Math.pow(10.0, a) }
    fun pow2(a: Double): Double { return Math.pow(2.0, a) }
    fun radTodDeg(a: Double): Double { return 180*a/Math.PI }
    fun sub(a: Double, b: Double): Double { return a-b }
    fun times2(a: Double): Double { return a*2 }
    fun xor(a: Double, b: Double): Double { return a.toLong().xor(b.toLong()).toDouble() }
}

enum class CalcOps {
    ACOS, ADD, AND, ASIN, ATAN, CEIL, CHS, COS, DEG_TO_RAD, DIV, DIV2,
    DP_FROM, EE, EPS_FROM, EXP, FLOOR, GCD, INV, LCM, LN, LOG10, LOG2,
    MOD, MUL, NOT, OR, PI, POW10, POW2, RAD_TO_DEG, ROUND, SIGN_CROP,
    SIGN_EXTEND, SIN, SQRT, SUB, TAN, TIMES2, TO_DP, TO_EPS, XOR, Y_POW_X;
    fun doOp(viewModel: CalcViewModel): () -> Unit {
        return when (this) {
            ACOS -> fun() { viewModel.unop({ a -> Math.acos(a) }) }
            ADD -> fun() { viewModel.binop({ a, b -> CalcMath.add(a,b) }) }
            AND -> fun() { viewModel.binop({ a, b -> CalcMath.and(a,b) }) }
            ASIN -> fun() { viewModel.unop({ a -> Math.asin(a) }) }
            ATAN -> fun() { viewModel.unop({ a -> Math.atan(a) }) }
            CEIL -> fun() { viewModel.unop({ a -> Math.ceil(a) }) }
            CHS -> fun() { viewModel.unop({ a -> CalcMath.chs(a) }) }
            COS -> fun() { viewModel.unop({ a -> Math.cos(a) }) }
            DEG_TO_RAD -> fun() { viewModel.unop({ a -> CalcMath.degToRad(a) }) }
            DIV -> fun() { viewModel.binop({ a, b -> CalcMath.div(a,b) }) }
            DIV2 -> fun() { viewModel.unop({ a -> CalcMath.div2(a) }) }
            DP_FROM -> fun() { viewModel.pushConstant(viewModel.everythingState.value.formatState.decimalPlaces.toDouble()) }
            EE -> fun() { viewModel.padAppendEE() }
            EPS_FROM -> fun() { viewModel.pushConstant(viewModel.everythingState.value.formatState.epsilon) }
            EXP -> fun() { viewModel.unop({ a -> Math.exp(a) }) }
            FLOOR -> fun() { viewModel.unop({ a -> Math.floor(a) }) }
            GCD -> fun() { viewModel.binop({ a, b -> CalcMath.gcd(a.toLong(),b.toLong()).toDouble() }) }
            INV -> fun() { viewModel.unop({ a -> CalcMath.inv(a) }) }
            LCM -> fun() { viewModel.binop({ a, b -> CalcMath.lcm(a.toLong(),b.toLong()).toDouble() }) }
            LN -> fun() { viewModel.unop({ a -> Math.log(a) }) }
            LOG10 -> fun() { viewModel.unop({ a -> CalcMath.log10(a) }) }
            LOG2 -> fun() { viewModel.unop({ a -> CalcMath.log2(a) }) }
            MOD -> fun() { viewModel.binop({ a, b -> CalcMath.mod(a,b) }) }
            MUL -> fun() { viewModel.binop({ a, b -> CalcMath.mul(a,b) }) }
            NOT -> fun() { viewModel.unop({ a -> CalcMath.not(a) }) }
            OR -> fun() { viewModel.binop({ a, b -> CalcMath.or(a,b) }) }
            PI -> fun() { viewModel.pushConstant(Math.PI) }
            POW10 -> fun() { viewModel.unop({ a -> CalcMath.pow10(a) }) }
            POW2 -> fun() { viewModel.unop({ a -> CalcMath.pow2(a) }) }
            RAD_TO_DEG -> fun() { viewModel.unop({ a -> CalcMath.radTodDeg(a) }) }
            ROUND -> fun() { viewModel.unop({ a -> Math.round(a).toDouble() }) }
            SIGN_CROP -> fun() { viewModel.unop({ a -> CalcMath.signCrop(a) }) }
            SIGN_EXTEND -> fun() { viewModel.unop({ a -> CalcMath.signExtend(a) }) }
            SIN -> fun() { viewModel.unop({ a -> Math.sin(a) }) }
            SQRT -> fun() { viewModel.unop({ a -> Math.sqrt(a) }) }
            SUB -> fun() { viewModel.binop({ a, b -> CalcMath.sub(a,b) }) }
            TAN -> fun() { viewModel.unop({ a -> Math.tan(a) }) }
            TIMES2 -> fun() { viewModel.unop({ a -> CalcMath.times2(a) }) }
            TO_DP -> fun() { viewModel.pop1op({ d -> viewModel.decimalPlacesSet(d.toInt()) }) }
            TO_EPS -> fun() { viewModel.pop1op({ e -> viewModel.epsilonSet(e) }) }
            XOR -> fun() { viewModel.binop({ a, b -> CalcMath.xor(a,b) }) }
            Y_POW_X -> fun() { viewModel.binop({ a, b -> Math.pow(a,b) }) }
        }
    }
}
