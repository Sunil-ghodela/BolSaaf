package com.bolsaaf.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize: Int by lazy {
        // Ensure buffer is at least FRAME_SIZE * 2 (bytes per frame)
        maxOf(AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat), FRAME_SIZE * 2 * 2)
    }

    private var onAudioDataCallback: ((ShortArray) -> Unit)? = null
    private var onRecordingFinished: ((File) -> Unit)? = null

    fun setCallbacks(
        onData: ((ByteArray) -> Unit)? = null,
        onFinished: ((File) -> Unit)? = null
    ) {
        this.onAudioDataCallback = onData
        this.onRecordingFinished = onFinished
    }

    fun startRecording(outputFile: File): Boolean {
        if (isRecording) return false

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            return false
        }

        isRecording = true
        audioRecord?.startRecording()

        recordingThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            recordAudio(outputFile)
        }.apply { start() }

        return true
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun isRecording(): Boolean = isRecording

    private fun recordAudio(outputFile: File) {
        val audioData = ByteArrayOutputStream()
        // RNNoise requires exactly 480 samples per frame
        val audioBuffer = ShortArray(FRAME_SIZE)

        while (isRecording) {
            val read = audioRecord?.read(audioBuffer, 0, FRAME_SIZE) ?: 0
            if (read == FRAME_SIZE) {
                // Process through RNNoise callback
                onAudioDataCallback?.invoke(audioBuffer.copyOf())
                
                // Write to output stream (convert shorts to bytes)
                val byteBuffer = ByteBuffer.allocate(FRAME_SIZE * 2)
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                audioBuffer.forEach { byteBuffer.putShort(it) }
                audioData.write(byteBuffer.array())
            }
        }

        // Save to WAV file
        writeWavFile(audioData.toByteArray(), outputFile)
        onRecordingFinished?.invoke(outputFile)
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
