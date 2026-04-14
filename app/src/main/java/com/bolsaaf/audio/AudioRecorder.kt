package com.bolsaaf.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import android.os.Process
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 48000
        const val FRAME_SIZE = 480  // RNNoise frame size (~10ms at 48kHz)
    }

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize: Int by lazy {
        // Ensure buffer is at least FRAME_SIZE * 2 (bytes per frame)
        maxOf(AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat), FRAME_SIZE * 2 * 2)
    }

    // Callbacks for processing
    private var onAudioDataCallback: ((ShortArray) -> Unit)? = null
    private var onRecordingFinished: ((File, File) -> Unit)? = null  // (originalFile, cleanedFile)

    // AudioProcessor reference for RNNoise
    private var audioProcessor: AudioProcessor? = null

    fun setCallbacks(
        onData: ((ShortArray) -> Unit)? = null,
        onFinished: ((File, File) -> Unit)? = null
    ) {
        this.onAudioDataCallback = onData
        this.onRecordingFinished = onFinished
    }

    fun setAudioProcessor(processor: AudioProcessor) {
        this.audioProcessor = processor
    }

    fun startRecording(originalFile: File, cleanedFile: File): Boolean {
        if (isRecording) return false

        // Prefer raw mic without OEM AEC/NS so RNNoise has real noise to remove.
        val sources = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                add(MediaRecorder.AudioSource.UNPROCESSED)
            }
            add(MediaRecorder.AudioSource.MIC)
            add(MediaRecorder.AudioSource.DEFAULT)
        }
        var record: AudioRecord? = null
        for (src in sources) {
            val ar = AudioRecord(
                src,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (ar.state == AudioRecord.STATE_INITIALIZED) {
                record = ar
                break
            }
            ar.release()
        }
        audioRecord = record

        if (audioRecord == null) {
            return false
        }

        audioRecord?.let { ar ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ar.sampleRate != SAMPLE_RATE) {
                Log.w(
                    "AudioRecorder",
                    "Mic opened at ${ar.sampleRate} Hz (requested $SAMPLE_RATE); resampling to 48k before RNNoise"
                )
            }
            if (NoiseSuppressor.isAvailable()) {
                try {
                    noiseSuppressor = NoiseSuppressor.create(ar.audioSessionId)?.also { it.enabled = true }
                } catch (_: Exception) {
                    noiseSuppressor = null
                }
            }
            if (AutomaticGainControl.isAvailable()) {
                try {
                    /* Off: AGC fights RNNoise + pushes peaks toward 0 dBFS */
                    automaticGainControl = AutomaticGainControl.create(ar.audioSessionId)?.also { it.enabled = false }
                } catch (_: Exception) {
                    automaticGainControl = null
                }
            }
        }

        isRecording = true
        audioRecord?.startRecording()

        recordingThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            recordAudio(originalFile, cleanedFile)
        }.apply { start() }

        return true
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
            // Already stopped or invalid state
        }
        // Wait for record loop to finish before release (avoid use-after-release crash)
        try {
            recordingThread?.join(10_000)
            if (recordingThread?.isAlive == true) {
                Log.w("AudioRecorder", "stopRecording: record thread still alive after 10s join; releasing anyway")
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        recordingThread = null
        try {
            noiseSuppressor?.release()
        } catch (_: Exception) {
        }
        noiseSuppressor = null
        try {
            automaticGainControl?.release()
        } catch (_: Exception) {
        }
        automaticGainControl = null
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
    }

    fun isRecording(): Boolean = isRecording

    private fun recordAudio(originalFile: File, cleanedFile: File) {
        val ar = audioRecord ?: return
        val actualRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ar.sampleRate
        } else {
            SAMPLE_RATE
        }
        val needSrc = PcmResample.requiredSourceSamples(actualRate)

        val originalAudioData = ByteArrayOutputStream()
        val cleanedAudioData = ByteArrayOutputStream()

        var accum = ShortArray(4096)
        var accumLen = 0
        val readShorts = (bufferSize / 2).coerceAtLeast(FRAME_SIZE * 8)
        val readBuf = ShortArray(readShorts)

        fun appendToAccum(src: ShortArray, n: Int) {
            if (accumLen + n > accum.size) {
                accum = accum.copyOf((accumLen + n).coerceAtLeast(accum.size * 2))
            }
            src.copyInto(accum, accumLen, 0, n)
            accumLen += n
        }

        fun normalizeToRnFrame(samples: ShortArray): ShortArray {
            if (samples.size == FRAME_SIZE) return samples
            return ShortArray(FRAME_SIZE) { i ->
                if (i < samples.size) samples[i] else 0
            }
        }

        fun processAndWrite(frame48: ShortArray) {
            val staged = frame48.copyOf()
            val preset = audioProcessor?.cleaningPreset ?: CleaningPreset.NORMAL
            AudioInputStage.apply(staged, preset)
            originalAudioData.write(shortsToBytes(staged))
            val cleaned = staged.copyOf()
            audioProcessor?.processStagedFrame(cleaned)
            cleanedAudioData.write(shortsToBytes(cleaned))
            onAudioDataCallback?.invoke(cleaned)
        }

        while (isRecording) {
            val read = ar.read(readBuf, 0, readBuf.size)
            if (read > 0) {
                appendToAccum(readBuf, read)
                while (accumLen >= needSrc) {
                    val srcChunk = accum.copyOfRange(0, needSrc)
                    val frame48 = normalizeToRnFrame(PcmResample.resampleMonoTo48k(srcChunk, actualRate))
                    processAndWrite(frame48)
                    accum.copyInto(accum, 0, needSrc, accumLen)
                    accumLen -= needSrc
                }
            }
        }

        if (accumLen > 0) {
            val padded = ShortArray(needSrc) { i -> if (i < accumLen) accum[i] else 0 }
            val frame48 = normalizeToRnFrame(PcmResample.resampleMonoTo48k(padded, actualRate))
            processAndWrite(frame48)
        }

        writeWavFile(originalAudioData.toByteArray(), originalFile)
        writeWavFile(cleanedAudioData.toByteArray(), cleanedFile)

        onRecordingFinished?.invoke(originalFile, cleanedFile)
    }

    private fun shortsToBytes(shortArray: ShortArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(shortArray.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        shortArray.forEach { byteBuffer.putShort(it) }
        return byteBuffer.array()
    }

    private fun writeWavFile(pcmData: ByteArray, outputFile: File) {
        val channels = 1
        val byteRate = SAMPLE_RATE * channels * 2
        val totalDataLen = pcmData.size + 36
        val totalAudioLen = pcmData.size

        FileOutputStream(outputFile).use { fos ->
            // Write WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(totalDataLen))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16))  // Subchunk1Size
            fos.write(shortToBytes(1))  // AudioFormat (PCM)
            fos.write(shortToBytes(channels.toShort()))
            fos.write(intToBytes(SAMPLE_RATE))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes((channels * 2).toShort()))
            fos.write(shortToBytes(16))  // BitsPerSample
            fos.write("data".toByteArray())
            fos.write(intToBytes(totalAudioLen))
            fos.write(pcmData)
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
