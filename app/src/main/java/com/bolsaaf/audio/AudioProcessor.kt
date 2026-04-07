package com.bolsaaf.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class AudioProcessor(private val context: Context) {

    private val rnnoise = RNNoise()
    private var statePtr: Long = 0
    private var isInitialized = false

    /** Input gain / limiter before RNNoise (same for live + file clean). */
    var cleaningPreset: CleaningPreset = CleaningPreset.NORMAL

    companion object {
        const val SAMPLE_RATE = 48000
        const val FRAME_SIZE = 480  // RNNoise frame size
        const val WAV_HEADER_BYTES = 44
    }

    fun initialize(): Boolean {
        statePtr = rnnoise.create()
        isInitialized = statePtr != 0L
        if (isInitialized) {
            rnnoise.resetPostState()
        } else {
            if (statePtr != 0L) rnnoise.destroy(statePtr)
            statePtr = 0
        }
        return isInitialized
    }

    fun destroy() {
        rnnoise.resetPostState()
        if (statePtr != 0L) {
            rnnoise.destroy(statePtr)
            statePtr = 0
        }
        isInitialized = false
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

    /** Decode any supported audio URI and export as 48k mono WAV for cloud API. */
    fun exportUriAsWav(inputUri: Uri, outputFile: File): Boolean {
        return try {
            val pcmData = extractPcmData(inputUri)
            writeWavFile(pcmData, outputFile, SAMPLE_RATE, 1)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Export URI as WAV and hard-cap the output size.
     * If audio is longer than allowed, keeps the beginning segment only.
     */
    fun exportUriAsWavCapped(inputUri: Uri, outputFile: File, maxBytes: Int): Boolean {
        return try {
            val pcmData = extractPcmData(inputUri)
            val maxPcmBytes = (maxBytes - WAV_HEADER_BYTES).coerceAtLeast(0)
            val maxSamples = (maxPcmBytes / 2).coerceAtLeast(0)
            val capped = if (pcmData.size > maxSamples) pcmData.copyOf(maxSamples) else pcmData
            writeWavFile(capped, outputFile, SAMPLE_RATE, 1)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Apply input staging + full studio chain (use from tests; live record uses [processStagedFrame] after manual staging). */
    fun processFrame(buffer: ShortArray) {
        require(buffer.size == FRAME_SIZE) { "Buffer must be exactly $FRAME_SIZE samples" }
        AudioInputStage.apply(buffer, cleaningPreset)
        processStagedFrame(buffer)
    }

    /** RNNoise + post chain only — input must already be staged. */
    fun processStagedFrame(buffer: ShortArray) {
        require(buffer.size == FRAME_SIZE) { "Buffer must be exactly $FRAME_SIZE samples" }
        if (isInitialized) {
            rnnoise.processStudio(statePtr, buffer)
        }
    }

    fun isInitialized(): Boolean = isInitialized

    /**
     * Prefer parsing WAV directly (correct PCM layout). Otherwise decode via MediaCodec
     * using actual sample rate, channel count, and float vs int PCM — the old code assumed
     * interleaved s16 mono, which turns float/stereo AAC into garbage ("jhur jhur").
     */
    private fun extractPcmData(uri: Uri): ShortArray {
        tryReadWavPcm(uri)?.let { return resampleTo48kMonoS16(it.samples, it.sampleRate, it.channels) }
        return decodeMediaCodecTo48kMonoS16(uri)
    }

    private data class WavPcm(val samples: ShortArray, val sampleRate: Int, val channels: Int)

    private fun tryReadWavPcm(uri: Uri): WavPcm? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        if (bytes.size < 44) return null
        if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
            bytes[2] != 'F'.code.toByte() || bytes[3] != 'F'.code.toByte()
        ) return null

        var offset = 12
        var audioFormat = 0
        var numChannels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var dataStart = -1
        var dataSize = 0

        chunkLoop@ while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
            val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val dataOffset = offset + 8
            when (chunkId) {
                "fmt " -> {
                    if (dataOffset + chunkSize > bytes.size) return null
                    val fmt = ByteBuffer.wrap(bytes, dataOffset, chunkSize).order(ByteOrder.LITTLE_ENDIAN)
                    audioFormat = fmt.short.toInt() and 0xffff
                    numChannels = fmt.short.toInt() and 0xffff
                    sampleRate = fmt.int
                    fmt.int // byte rate skip
                    fmt.short // block align skip
                    bitsPerSample = fmt.short.toInt() and 0xffff
                }
                "data" -> {
                    dataStart = dataOffset
                    dataSize = chunkSize
                    break@chunkLoop
                }
            }
            offset = dataOffset + chunkSize + (chunkSize and 1)
        }
        if (dataStart < 0 || dataSize <= 0 || numChannels < 1) return null
        if (audioFormat != 1) return null // PCM only for direct path; else fall through to MediaCodec

        val samples: ShortArray = when (bitsPerSample) {
            16 -> {
                val n = dataSize / 2
                val out = ShortArray(n)
                ByteBuffer.wrap(bytes, dataStart, dataSize).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out)
                out
            }
            24 -> {
                val frames = dataSize / (3 * numChannels)
                val out = ShortArray(frames * numChannels)
                var p = dataStart
                for (i in out.indices) {
                    val b0 = bytes[p++].toInt() and 0xff
                    val b1 = bytes[p++].toInt() and 0xff
                    val b2 = bytes[p++].toInt() and 0xff
                    var v = b0 or (b1 shl 8) or (b2 shl 16)
                    if ((v and 0x800000) != 0) v = v or 0xff000000.toInt()
                    out[i] = (v shr 8).toShort()
                }
                out
            }
            else -> return null
        }
        return WavPcm(samples, sampleRate, numChannels)
    }

    private fun resampleTo48kMonoS16(interleaved: ShortArray, srcRate: Int, channels: Int): ShortArray {
        val mono = if (channels == 1) {
            interleaved
        } else {
            val frames = interleaved.size / channels
            ShortArray(frames) { fi ->
                var sum = 0L
                for (c in 0 until channels) {
                    sum += interleaved[fi * channels + c].toInt()
                }
                (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        if (srcRate == SAMPLE_RATE) return mono
        if (srcRate <= 0) return mono
        val srcLen = mono.size
        val dstLen = ((srcLen.toLong() * SAMPLE_RATE + srcRate / 2) / srcRate).toInt().coerceAtLeast(1)
        return ShortArray(dstLen) { di ->
            val srcPos = (di.toDouble() * srcRate) / SAMPLE_RATE
            val i0 = srcPos.toInt().coerceIn(0, srcLen - 1)
            val i1 = (i0 + 1).coerceAtMost(srcLen - 1)
            val frac = (srcPos - i0).toFloat()
            val f0 = mono[i0].toFloat()
            val f1 = mono[i1].toFloat()
            (f0 + (f1 - f0) * frac).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun decodeMediaCodecTo48kMonoS16(uri: Uri): ShortArray {
        val extractor = MediaExtractor()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            extractor.setDataSource(pfd.fileDescriptor)
        } ?: throw IllegalStateException("Cannot open URI")

        val format = extractor.getTrackFormat(0)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("No MIME")

        if (!mime.startsWith("audio/")) {
            throw IllegalArgumentException("Not an audio file")
        }

        extractor.selectTrack(0)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false

        var outEncoding = AudioFormat.ENCODING_PCM_16BIT
        var outChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var outSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

        val floatChunks = ArrayList<FloatArray>()
        val shortChunks = ArrayList<ShortArray>()
        var totalFloatSamples = 0
        var totalShortSamples = 0

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inputBufferId = codec.dequeueInputBuffer(10_000)
                if (inputBufferId >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        codec.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFmt = codec.outputFormat
                    if (newFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        outEncoding = newFmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                    if (newFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        outChannels = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (newFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        outSampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                }
                outputBufferId >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputBufferId)!!
                    if (bufferInfo.size > 0) {
                        when (outEncoding) {
                            AudioFormat.ENCODING_PCM_FLOAT -> {
                                outputBuffer.order(ByteOrder.nativeOrder())
                                val fb = outputBuffer.asFloatBuffer()
                                val n = fb.remaining()
                                val arr = FloatArray(n)
                                fb.get(arr)
                                floatChunks.add(arr)
                                totalFloatSamples += n
                            }
                            else -> {
                                val chunk = ByteArray(bufferInfo.size)
                                outputBuffer.get(chunk)
                                val n = chunk.size / 2
                                val shorts = ShortArray(n)
                                ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                                shortChunks.add(shorts)
                                totalShortSamples += n
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferId, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val interleavedShort: ShortArray = if (floatChunks.isNotEmpty()) {
            val all = FloatArray(totalFloatSamples)
            var pos = 0
            for (c in floatChunks) {
                c.copyInto(all, pos)
                pos += c.size
            }
            ShortArray(all.size) { i ->
                (all[i] * 32767f).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        } else {
            val all = ShortArray(totalShortSamples)
            var p = 0
            for (c in shortChunks) {
                c.copyInto(all, p)
                p += c.size
            }
            all
        }

        return resampleTo48kMonoS16(interleavedShort, outSampleRate, outChannels)
    }

    private fun processAudioBuffer(pcmData: ShortArray, progressCallback: (Int) -> Unit): ShortArray {
        val frameSize = FRAME_SIZE
        val numFrames = pcmData.size / frameSize
        val outputBuffer = ShortArray(pcmData.size)

        for (i in 0 until numFrames) {
            val offset = i * frameSize
            val frame = pcmData.copyOfRange(offset, offset + frameSize)
            AudioInputStage.apply(frame, cleaningPreset)
            rnnoise.processStudio(statePtr, frame)

            frame.copyInto(outputBuffer, offset)
            progressCallback((i * 100) / numFrames.coerceAtLeast(1))
        }

        val remaining = pcmData.size % frameSize
        if (remaining > 0) {
            val start = numFrames * frameSize
            pcmData.copyInto(outputBuffer, start, start, start + remaining)
        }

        progressCallback(100)
        return outputBuffer
    }

    private fun writeWavFile(pcmData: ShortArray, outputFile: File, sampleRate: Int, channels: Int) {
        val byteRate = sampleRate * channels * 2
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36

        FileOutputStream(outputFile).use { fos ->
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
