package com.reelvoice.video

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Decodes a user-picked image into a right-sized Bitmap for the waveform
 * encoder. Uses [BitmapFactory.Options.inSampleSize] (power-of-2 downsample)
 * to keep a 24 MP phone photo from being decoded at full resolution — the
 * encoder only needs ~2K pixels either way.
 */
object PhotoLoader {

    /** Decode [uri] to a Bitmap no larger than 2× the target video frame. */
    fun loadScaled(
        contentResolver: ContentResolver,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        require(srcW > 0 && srcH > 0) { "Couldn't read image dimensions" }

        var sample = 1
        while (srcW / (sample * 2) >= targetWidth && srcH / (sample * 2) >= targetHeight) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        } ?: error("Couldn't decode image from $uri")
        return bitmap
    }
}
