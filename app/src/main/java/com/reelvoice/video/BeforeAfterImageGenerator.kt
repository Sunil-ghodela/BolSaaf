package com.reelvoice.video

import android.graphics.Bitmap
import android.graphics.Canvas
import com.reelvoice.audio.WavPreview
import java.io.File
import java.io.FileOutputStream

/**
 * Builds the static Before/After share PNG. Loads RMS bars for the noisy
 * original + cleaned output via [WavPreview], hands them to
 * [BeforeAfterImageRenderer], writes a PNG to disk.
 *
 * Call off the main thread — Bitmap creation + PNG compress is ~100-400 ms
 * depending on device.
 */
object BeforeAfterImageGenerator {

    enum class Aspect(val outWidth: Int, val outHeight: Int) {
        /** IG Story / WhatsApp Status / YouTube Shorts cover. */
        PORTRAIT_9_16(1080, 1920),
        /** IG feed / WhatsApp chat image. */
        SQUARE_1_1(1080, 1080)
    }

    fun generate(
        originalWav: File,
        cleanedWav: File,
        output: File,
        aspect: Aspect = Aspect.PORTRAIT_9_16,
        barCount: Int = 48
    ): File {
        val beforeBars = WavPreview.loadBars(originalWav, barCount).toFloatArrayCompat()
        val afterBars = WavPreview.loadBars(cleanedWav, barCount).toFloatArrayCompat()

        val bitmap = Bitmap.createBitmap(aspect.outWidth, aspect.outHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val renderer = BeforeAfterImageRenderer(aspect.outWidth, aspect.outHeight)
            renderer.render(canvas, beforeBars, afterBars)

            output.parentFile?.mkdirs()
            FileOutputStream(output).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
        } finally {
            bitmap.recycle()
        }
        return output
    }

    private fun List<Float>.toFloatArrayCompat(): FloatArray {
        val out = FloatArray(size)
        for (i in indices) out[i] = this[i]
        return out
    }
}
