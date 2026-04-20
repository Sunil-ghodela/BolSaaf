package com.reelvoice.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Biquad filter — stateful IIR with coefficients from the Audio EQ Cookbook
 * (Robert Bristow-Johnson). Pure Kotlin, no Android deps.
 *
 * Use [Coefficients] factory helpers to pick filter type/frequency/gain, then
 * instantiate a [BiquadFilter] per channel (single-channel mono here) and
 * pump samples through [BiquadFilter.process].
 */
object Biquad {

    data class Coefficients(
        val b0: Double,
        val b1: Double,
        val b2: Double,
        val a1: Double,
        val a2: Double,
    )

    fun highPass(sampleRate: Int, freq: Double, q: Double = 0.707): Coefficients {
        val (cosW, alpha) = trig(sampleRate, freq, q)
        val a0 = 1.0 + alpha
        val b0 = (1.0 + cosW) / 2.0 / a0
        val b1 = -(1.0 + cosW) / a0
        val b2 = (1.0 + cosW) / 2.0 / a0
        val a1 = -2.0 * cosW / a0
        val a2 = (1.0 - alpha) / a0
        return Coefficients(b0, b1, b2, a1, a2)
    }

    fun lowShelf(sampleRate: Int, freq: Double, gainDb: Double, q: Double = 0.707): Coefficients {
        val A = 10.0.pow(gainDb / 40.0)
        val (cosW, _) = trig(sampleRate, freq, q)
        val sinW = sin(2.0 * PI * freq / sampleRate)
        val alpha = sinW / 2.0 * sqrt((A + 1.0 / A) * (1.0 / q - 1.0) + 2.0)
        val a0 = (A + 1) + (A - 1) * cosW + 2 * sqrt(A) * alpha
        val b0 = A * ((A + 1) - (A - 1) * cosW + 2 * sqrt(A) * alpha) / a0
        val b1 = 2 * A * ((A - 1) - (A + 1) * cosW) / a0
        val b2 = A * ((A + 1) - (A - 1) * cosW - 2 * sqrt(A) * alpha) / a0
        val a1 = -2 * ((A - 1) + (A + 1) * cosW) / a0
        val a2 = ((A + 1) + (A - 1) * cosW - 2 * sqrt(A) * alpha) / a0
        return Coefficients(b0, b1, b2, a1, a2)
    }

    fun highShelf(sampleRate: Int, freq: Double, gainDb: Double, q: Double = 0.707): Coefficients {
        val A = 10.0.pow(gainDb / 40.0)
        val (cosW, _) = trig(sampleRate, freq, q)
        val sinW = sin(2.0 * PI * freq / sampleRate)
        val alpha = sinW / 2.0 * sqrt((A + 1.0 / A) * (1.0 / q - 1.0) + 2.0)
        val a0 = (A + 1) - (A - 1) * cosW + 2 * sqrt(A) * alpha
        val b0 = A * ((A + 1) + (A - 1) * cosW + 2 * sqrt(A) * alpha) / a0
        val b1 = -2 * A * ((A - 1) + (A + 1) * cosW) / a0
        val b2 = A * ((A + 1) + (A - 1) * cosW - 2 * sqrt(A) * alpha) / a0
        val a1 = 2 * ((A - 1) - (A + 1) * cosW) / a0
        val a2 = ((A + 1) - (A - 1) * cosW - 2 * sqrt(A) * alpha) / a0
        return Coefficients(b0, b1, b2, a1, a2)
    }

    fun peakingEq(sampleRate: Int, freq: Double, gainDb: Double, q: Double = 1.0): Coefficients {
        val A = 10.0.pow(gainDb / 40.0)
        val (cosW, alpha) = trig(sampleRate, freq, q)
        val a0 = 1.0 + alpha / A
        val b0 = (1.0 + alpha * A) / a0
        val b1 = -2.0 * cosW / a0
        val b2 = (1.0 - alpha * A) / a0
        val a1 = -2.0 * cosW / a0
        val a2 = (1.0 - alpha / A) / a0
        return Coefficients(b0, b1, b2, a1, a2)
    }

    private fun trig(sampleRate: Int, freq: Double, q: Double): Pair<Double, Double> {
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val alpha = sinW / (2.0 * q)
        return cosW to alpha
    }
}

class BiquadFilter(private val c: Biquad.Coefficients) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(x: Double): Double {
        val y = c.b0 * x + c.b1 * x1 + c.b2 * x2 - c.a1 * y1 - c.a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }

    fun reset() {
        x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
    }
}
