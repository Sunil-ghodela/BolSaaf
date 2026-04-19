package com.reelvoice.video

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.max

/**
 * Paints a single frame of the waveform reel onto an Android [Canvas]. Kept separate
 * from [WaveformVideoEncoder] so the MediaCodec wiring stays small and so the paint
 * logic can be tweaked without touching the encoder.
 *
 * Style: brand-gradient background, centered vertical bars that grow/shrink to the
 * per-frame amplitude envelope, optional title + subtitle text. Matches the app's
 * light theme and BrandGradient stops (red → purple → blue).
 */
class WaveformFrameRenderer(
    private val width: Int,
    private val height: Int,
    private val title: String? = null,
    private val subtitle: String? = null
) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(BRAND_RED_SOFT, BRAND_PURPLE_SOFT, BRAND_BLUE_SOFT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, height * 0.25f, 0f, height * 0.75f,
            intArrayOf(BRAND_RED, BRAND_PURPLE, BRAND_BLUE),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = height * 0.042f
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7280.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
        textSize = height * 0.026f
    }

    private val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66000000.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = height * 0.018f
    }

    private val barRect = RectF()

    fun render(canvas: Canvas, amplitudes: FloatArray) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val barCount = amplitudes.size
        if (barCount == 0) return

        val horizontalPadding = width * 0.08f
        val usableWidth = width - 2 * horizontalPadding
        val slotWidth = usableWidth / barCount
        val barWidth = slotWidth * 0.6f
        val barGap = slotWidth - barWidth
        val cornerRadius = barWidth * 0.5f

        val centerY = height * 0.5f
        val maxHalfHeight = height * 0.25f
        val minHalfHeight = height * 0.008f

        for (i in 0 until barCount) {
            val amp = amplitudes[i].coerceIn(0f, 1f)
            val halfHeight = max(minHalfHeight, amp * maxHalfHeight)
            val left = horizontalPadding + i * slotWidth + barGap * 0.5f
            barRect.set(left, centerY - halfHeight, left + barWidth, centerY + halfHeight)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
        }

        title?.let {
            canvas.drawText(it, width * 0.5f, height * 0.18f, titlePaint)
        }
        subtitle?.let {
            canvas.drawText(it, width * 0.5f, height * 0.23f, subtitlePaint)
        }
        canvas.drawText(WATERMARK, width * 0.5f, height * 0.94f, watermarkPaint)
    }

    companion object {
        private const val WATERMARK = "Made with ReelVoice"
        private const val BRAND_RED = 0xFFE94E5B.toInt()
        private const val BRAND_PURPLE = 0xFFA24CB7.toInt()
        private const val BRAND_BLUE = 0xFF3D7DDB.toInt()
        private const val BRAND_RED_SOFT = 0xFFFDEBEE.toInt()
        private const val BRAND_PURPLE_SOFT = 0xFFF4E8F7.toInt()
        private const val BRAND_BLUE_SOFT = 0xFFE8F1FB.toInt()
    }
}
