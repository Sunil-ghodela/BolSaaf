package com.reelvoice.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.max

/**
 * [FrameRenderer] that paints the user's uploaded photo as a center-cropped
 * background and overlays an animated waveform in the lower third.
 *
 * A top and bottom vertical scrim keep the title + bars readable even over
 * bright photos. Bars are rendered in white (semi-translucent) so they read
 * against both light and dark backgrounds.
 */
class PhotoWaveformFrameRenderer(
    private val width: Int,
    private val height: Int,
    private val background: Bitmap,
    private val title: String? = null,
    private val subtitle: String? = null,
) : FrameRenderer {

    private val w = width.toFloat()
    private val h = height.toFloat()

    private val srcRect: Rect
    private val dstRect = Rect(0, 0, width, height)

    init {
        val bw = background.width.toFloat()
        val bh = background.height.toFloat()
        val scale = maxOf(w / bw, h / bh) // fill (center-crop)
        val cropW = (w / scale).coerceAtMost(bw)
        val cropH = (h / scale).coerceAtMost(bh)
        val left = ((bw - cropW) / 2f).toInt().coerceAtLeast(0)
        val top = ((bh - cropH) / 2f).toInt().coerceAtLeast(0)
        srcRect = Rect(
            left,
            top,
            (left + cropW.toInt()).coerceAtMost(background.width),
            (top + cropH.toInt()).coerceAtMost(background.height)
        )
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val topScrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, h * 0.32f,
            intArrayOf(0xAA000000.toInt(), 0x00000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private val bottomScrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, h * 0.45f, 0f, h,
            intArrayOf(0x00000000, 0xB3000000.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, h * 0.58f, 0f, h * 0.90f,
            intArrayOf(0xFFFFFFFF.toInt(), 0xCCFFFFFF.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = h * 0.044f
        letterSpacing = 0.06f
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6FFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
        textSize = h * 0.024f
    }

    private val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = h * 0.020f
        letterSpacing = 0.10f
    }

    private val barRect = RectF()

    override fun render(canvas: Canvas, amplitudes: FloatArray) {
        canvas.drawBitmap(background, srcRect, dstRect, bgPaint)
        canvas.drawRect(0f, 0f, w, h * 0.32f, topScrimPaint)
        canvas.drawRect(0f, h * 0.45f, w, h, bottomScrimPaint)

        val barCount = amplitudes.size
        if (barCount > 0) {
            val horizontalPadding = w * 0.08f
            val usableWidth = w - 2 * horizontalPadding
            val slotWidth = usableWidth / barCount
            val barWidth = slotWidth * 0.6f
            val barGap = slotWidth - barWidth
            val cornerRadius = barWidth * 0.5f
            val centerY = h * 0.75f
            val maxHalfHeight = h * 0.15f
            val minHalfHeight = h * 0.006f

            for (i in 0 until barCount) {
                val amp = amplitudes[i].coerceIn(0f, 1f)
                val halfHeight = max(minHalfHeight, amp * maxHalfHeight)
                val left = horizontalPadding + i * slotWidth + barGap * 0.5f
                barRect.set(left, centerY - halfHeight, left + barWidth, centerY + halfHeight)
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
            }
        }

        title?.let {
            canvas.drawText(it, w * 0.5f, h * 0.12f, titlePaint)
        }
        subtitle?.let {
            canvas.drawText(it, w * 0.5f, h * 0.16f, subtitlePaint)
        }
        canvas.drawText("Made with ReelVoice", w * 0.5f, h * 0.95f, watermarkPaint)
    }
}
