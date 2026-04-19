package com.reelvoice.audio

import android.util.Log

class RNNoise {

    companion object {
        const val FRAME_SIZE = 480  // RNNoise works with 480 samples per frame

        @Volatile
        private var loadError: Throwable? = null

        @Volatile
        private var libraryLoaded: Boolean = false

        init {
            try {
                System.loadLibrary("rnnoise-lib")
                libraryLoaded = true
                Log.i("RNNoise", "Native library loaded")
            } catch (t: Throwable) {
                loadError = t
                Log.e("RNNoise", "Failed to load native library — cloud fallback required", t)
            }
        }

        /** True when librnnoise-lib.so loaded successfully. Callers should fall back to cloud clean when false. */
        fun isLibraryLoaded(): Boolean = libraryLoaded

        /** The underlying load error if [isLibraryLoaded] is false, for diagnostic surfacing. */
        fun loadError(): Throwable? = loadError
    }

    external fun create(): Long
    external fun destroy(ptr: Long)
    external fun process(ptr: Long, audio: ShortArray)

    /** Phase 1+2: HPF -> pre-gain -> RNNoise -> dry mix -> post-gain -> EQ -> de-esser -> compressor. */
    external fun processStudio(ptr: Long, audio: ShortArray)

    /** Clear IIR state when starting a new session. */
    external fun resetPostState()
}
