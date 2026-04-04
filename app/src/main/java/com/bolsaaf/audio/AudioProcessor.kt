package com.bolsaaf.audio

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioProcessor(private val context: Context) {

    private val rnnoise = RNNoise()
    private var statePtr: Long = 0
    private var isInitialized = false

    companion object {
        const val SAMPLE_RATE = 48000
        const val FRAME_SIZE = 480  // RNNoise frame size
    }

    fun initialize(): Boolean {
        statePtr = rnnoise.create()
        isInitialized = statePtr != 0L
        return isInitialized
    }

    fun destroy() {
        if (statePtr != 0L) {
            rnnoise.destroy(statePtr)
            statePtr = 0
            isInitialized = false
        }
    }

    fun cleanAudioFile(inputUri: Uri, outputFile: File, progressCallback: (Int) -> Unit): Boolean {
        return try {
            val pcmData = extractPcmData(inputUri)
            val cleanedData = processAudioBuffer(pcmData, progressCallback)
            writeWavFile(cleanedData, outputFile, SAMPLE_RATE, 1)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun processFrame(buffer: ShortArray) {
        require(buffer.size == FRAME_SIZE) { "Buffer must be exactly $FRAME_SIZE samples" }
        if (isInitialized) {
            rnnoise.process(statePtr, buffer)
        }
    }

    fun isInitialized(): Boolean = isInitialized

    private fun extractPcmData(uri: Uri): ShortArray {
        val extractor = MediaExtractor()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            extractor.setDataSource(pfd.fileDescriptor)
        }

        val format = extractor.getTrackFormat(0)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

        if (!mime.startsWith("audio/")) {
            throw IllegalArgumentException("Not an audio file")
        }

        extractor.selectTrack(0)

        val outputStream = ByteArrayOutputStream()
        val buffer = ByteBuffer.allocate(1024 * 1024)

        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            buffer.flip()
            val chunk = ByteArray(sampleSize)
            buffer.get(chunk)
            outputStream.write(chunk)

            extractor.advance()
        }

        extractor.release()
        
        // Convert bytes to shorts
        val byteArray = outputStream.toByteArray()
        val numSamples = byteArray.size / 2
        val shortArray = ShortArray(numSamples)
        ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
        
        return shortArray
    }

    private fun processAudioBuffer(pcmData: ShortArray, progressCallback: (Int) -> Unit): ShortArray {
        val frameSize = FRAME_SIZE
        val numFrames = pcmData.size / frameSize
        val outputBuffer = ShortArray(pcmData.size)

        for (i in 0 until numFrames) {
            val offset = i * frameSize
            val frame = pcmData.copyOfRange(offset, offset + frameSize)
            
            rnnoise.process(statePtr, frame)
            
            frame.copyInto(outputBuffer, offset)
            progressCallback((i * 100) / numFrames)
        }

        // Copy remaining samples as-is
        val remaining = pcmData.size % frameSize
        if (remaining > 0) {
            val start = numFrames * frameSize
            for (i in 0 until remaining) {
                outputBuffer[start + i] = pcmData[start + i]
            }
        }

        progressCallback(100)
        return outputBuffer
    }

    private fun writeWavFile(pcmData: ShortArray, outputFile: File, sampleRate: Int, channels: Int) {
        val byteRate = sampleRate * channels * 2
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36

        FileOutputStream(outputFile).use { fos ->
            // Write WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(totalDataLen))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16))  // Subchunk1Size
            fos.write(shortToBytes(1))  // AudioFormat (PCM)
            fos.write(shortToBytes(channels.toShort()))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes((channels * 2).toShort()))
            fos.write(shortToBytes(16))  // BitsPerSample
            fos.write("data".toByteArray())
            fos.write(intToBytes(totalAudioLen))

            // Convert shorts to bytes and write
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            pcmData.forEach { byteBuffer.putShort(it) }
            fos.write(byteBuffer.array())
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
