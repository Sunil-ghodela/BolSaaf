package com.reelvoice.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper

interface BackgroundPreviewPlayer {
    fun play(url: String, onPlaying: () -> Unit, onComplete: () -> Unit, onError: () -> Unit)
    fun stop()
}

/** One asynchronous sample at a time. Invalidated callbacks cannot restart a released player. */
class AndroidBackgroundPreviewPlayer(context: Context) : BackgroundPreviewPlayer {
    private val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null
    private var timeout: Runnable? = null
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val lock = Any()

    override fun play(url: String, onPlaying: () -> Unit, onComplete: () -> Unit, onError: () -> Unit) {
        stop()
        // setDataSource can block on slow/unreachable HTTP — never do it on the UI thread.
        // MediaPlayer is created on this worker; with no Looper here, its event callbacks are
        // delivered on the main looper, so Compose state updates stay main-thread-safe.
        Thread {
            val next = MediaPlayer()
            var accepted = false
            synchronized(lock) {
                if (Thread.interrupted()) return@Thread
                player = next
                accepted = true
            }
            if (!accepted) { next.release(); return@Thread }
            fun fail() {
                if (synchronized(lock) { player !== next }) return
                stop()
                onError()
            }
            try {
                next.setAudioAttributes(attributes)
                next.setOnErrorListener { _, _, _ -> fail(); true }
                next.setOnCompletionListener {
                    if (synchronized(lock) { player === next }) { stop(); onComplete() }
                }
                next.setOnPreparedListener {
                    if (synchronized(lock) { player !== next }) return@setOnPreparedListener
                    timeout?.let(handler::removeCallbacks)
                    timeout = null
                    try {
                        val listener = AudioManager.OnAudioFocusChangeListener { change ->
                            if (change != AudioManager.AUDIOFOCUS_GAIN) fail()
                        }
                        focusListener = listener
                        val result = if (Build.VERSION.SDK_INT >= 26) {
                            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                                .setAudioAttributes(attributes).setOnAudioFocusChangeListener(listener, handler).build()
                            focusRequest = request
                            audio.requestAudioFocus(request)
                        } else {
                            @Suppress("DEPRECATION")
                            audio.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        }
                        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { fail() }
                        else { next.start(); onPlaying() }
                    } catch (_: Exception) { fail() }
                }
                next.setDataSource(url)
                timeout = Runnable { fail() }.also { handler.postDelayed(it, 15_000) }
                next.prepareAsync()
            } catch (_: Exception) { fail() }
        }.start()
    }

    override fun stop() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        val old = synchronized(lock) {
            val previous = player
            player = null
            previous
        } ?: return
        old.setOnPreparedListener(null)
        old.setOnCompletionListener(null)
        old.setOnErrorListener(null)
        old.release()
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let(audio::abandonAudioFocusRequest)
        else focusListener?.let {
            @Suppress("DEPRECATION")
            audio.abandonAudioFocus(it)
        }
        focusRequest = null
        focusListener = null
    }
}
