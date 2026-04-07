package com.bolsaaf.audio

import android.util.Log

class RNNoise {

    companion object {
        const val FRAME_SIZE = 480  // RNNoise works with 480 samples per frame

        init {
            System.loadLibrary("rnnoise-lib")
            Log.i("RNNoise", "Native library loaded")
        }
    }

    external fun create(): Long
    external fun destroy(ptr: Long)
    external fun process(ptr: Long, audio: ShortArray)

    /** Phase 1+2: HPF -> pre-gain -> RNNoise -> dry mix -> post-gain -> EQ -> de-esser -> compressor. */
    external fun processStudio(ptr: Long, audio: ShortArray)

    /** Clear IIR state when starting a new session. */
    external fun resetPostState()
}
