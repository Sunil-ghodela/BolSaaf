package com.bolsaaf.audio

class RNNoiseBridge {

    init {
        System.loadLibrary("rnnoise-jni")
    }

    private var nativeHandle: Long = 0

    companion object {
        const val FRAME_SIZE = 480  // RNNoise expects 480 samples at 48kHz
    }

    fun initialize(): Boolean {
        nativeHandle = nativeCreate()
        return nativeHandle != 0L
    }

    fun destroy() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }

    fun processFrame(input: FloatArray, output: FloatArray): Float {
        require(nativeHandle != 0L) { "RNNoise not initialized" }
        require(input.size == FRAME_SIZE) { "Input must be $FRAME_SIZE samples" }
        require(output.size == FRAME_SIZE) { "Output must be $FRAME_SIZE samples" }
        
        return nativeProcessFrame(nativeHandle, input, output)
    }

    fun processBuffer(pcmData: ByteArray, sampleRate: Int = 48000): ByteArray {
        require(nativeHandle != 0L) { "RNNoise not initialized" }
        return nativeProcessBuffer(nativeHandle, pcmData, sampleRate) ?: pcmData
    }

    // Native methods
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(statePtr: Long)
    private external fun nativeProcessFrame(statePtr: Long, input: FloatArray, output: FloatArray): Float
    private external fun nativeProcessBuffer(statePtr: Long, pcmData: ByteArray, sampleRate: Int): ByteArray?

    protected fun finalize() {
        destroy()
    }
}
