package com.reelvoice.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal 16-bit PCM WAV writer. Counterpart to the read helpers in
 * [WavPreview] and [com.reelvoice.video.WaveformWindowSampler.decodeWav].
 *
 * Lives alongside the audio layer so features that need to mutate a WAV
 * (silence cutter, filler remover, trimmer…) don't have to go through
 * [AudioProcessor]'s private writer.
 */
object WavIo {
    fun writeWav(
        pcm: ShortArray,
        output: File,
        sampleRate: Int,
        channels: Int = 1,
    ) {
        val byteRate = sampleRate * channels * 2
        val totalAudioLen = pcm.size * 2
        val totalDataLen = totalAudioLen + 36

        output.parentFile?.mkdirs()
        FileOutputStream(output).use { fos ->
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(totalDataLen))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16))
            fos.write(shortToBytes(1))
            fos.write(shortToBytes(channels.toShort()))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes((channels * 2).toShort()))
            fos.write(shortToBytes(16))
            fos.write("data".toByteArray())
            fos.write(intToBytes(totalAudioLen))

            val buf = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            pcm.forEach { buf.putShort(it) }
            fos.write(buf.array())
        }
    }

    private fun intToBytes(v: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()

    private fun shortToBytes(v: Short): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array()
}
