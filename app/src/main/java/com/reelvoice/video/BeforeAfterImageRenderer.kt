package com.reelvoice.video

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.max

/**
 * Paints a "Before / After" share card onto a [Canvas]. Top pane shows the
 * noisy original in muted grey, bottom pane shows the cleaned output in the
 * brand gradient — the visual delta is the whole point of sharing.
 *
 * Aspect-agnostic: layout is proportional to [width] / [height] so the same
 * renderer produces 9:16 (1080×1920) and 1:1 (1080×1080) crops.
 */
class BeforeAfterImageRenderer(
    private val width: Int,
    private val height: Int
) {
    private val w = width.toFloat()
    private val h = height.toFloat()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(BRAND_RED_SOFT, BRAND_PURPLE_SOFT, BRAND_BLUE_SOFT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private val beforeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9CA3AF.toInt()
        style = Paint.Style.FILL
    }

    private val afterBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(BRAND_RED, BRAND_PURPLE, BRAND_BLUE),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }

    private val beforeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7280.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = h * 0.030f
        letterSpacing = 0.22f
    }

    private val afterLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BRAND_PURPLE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = h * 0.040f
        letterSpacing = 0.22f
    }

    private val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99000000.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = h * 0.022f
        letterSpacing = 0.08f
    }

    private val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55000000.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
        textSize = h * 0.016f
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22000000.toInt()
        strokeWidth = h * 0.002f
    }

    private val barRect = RectF()

    fun render(canvas: Canvas, beforeAmps: FloatArray, afterAmps: FloatArray) {
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Pane windows as fractions of H — same numbers produce 9:16 & 1:1 equally well.
        val beforeLabelY = h * 0.100f
        val beforePaneTop = h * 0.135f
        val beforePaneBottom = h * 0.450f
        val dividerY = h * 0.495f
        val afterLabelY = h * 0.555f
        val afterPaneTop = h * 0.585f
        val afterPaneBottom = h * 0.880f
        val watermarkY = h * 0.945f
        val tagY = h * 0.975f

        canvas.drawText("BEFORE  ·  noisy", w * 0.5f, beforeLabelY, beforeLabelPaint)
        drawBars(canvas, beforeAmps, beforePaneTop, beforePaneBottom, beforeBarPaint)

        canvas.drawLine(w * 0.22f, dividerY, w * 0.78f, dividerY, dividerPaint)

        canvas.drawText("AFTER  ·  ReelVoice", w * 0.5f, afterLabelY, afterLabelPaint)
        drawBars(canvas, afterAmps, afterPaneTop, afterPaneBottom, afterBarPaint)

        canvas.drawText("Made with ReelVoice", w * 0.5f, watermarkY, watermarkPaint)
        canvas.drawText("reelvoice · clean voice, clean reels", w * 0.5f, tagY, tagPaint)
    }

    private fun drawBars(
        canvas: Canvas,
        amps: FloatArray,
        top: Float,
        bottom: Float,
        paint: Paint
    ) {
        val barCount = amps.size
        if (barCount == 0) return
        val horizontalPadding = w * 0.08f
        val usableWidth = w - 2 * horizontalPadding
        val slotWidth = usableWidth / barCount
        val barWidth = slotWidth * 0.6f
        val barGap = slotWidth - barWidth
        val cornerRadius = barWidth * 0.5f
        val centerY = (top + bottom) * 0.5f
        val maxHalfHeight = (bottom - top) * 0.48f
        val minHalfHeight = (bottom - top) * 0.03f

        for (i in 0 until barCount) {
            val amp = amps[i].coerceIn(0f, 1f)
            val halfHeight = max(minHalfHeight, amp * maxHalfHeight)
            val left = horizontalPadding + i * slotWidth + barGap * 0.5f
            barRect.set(left, centerY - halfHeight, left + barWidth, centerY + halfHeight)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paint)
        }
    }

    companion object {
        private const val BRAND_RED = 0xFFE94E5B.toInt()
        private const val BRAND_PURPLE = 0xFFA24CB7.toInt()
        private const val BRAND_BLUE = 0xFF3D7DDB.toInt()
        private const val BRAND_RED_SOFT = 0xFFFDEBEE.toInt()
        private const val BRAND_PURPLE_SOFT = 0xFFF4E8F7.toInt()
        private const val BRAND_BLUE_SOFT = 0xFFE8F1FB.toInt()
    }
}
