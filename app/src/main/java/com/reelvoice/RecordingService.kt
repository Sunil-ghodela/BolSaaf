package com.reelvoice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.reelvoice.audio.AudioProcessor
import com.reelvoice.audio.AudioRecorder
import com.reelvoice.audio.CleaningPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Foreground service that owns the microphone recording pipeline.
 *
 * The service (not the Activity) owns the [AudioRecorder] and [AudioProcessor], so recording
 * survives activity backgrounding, recreation, and configuration changes. Output paths are
 * passed via the start intent; on process death [START_REDELIVER_INTENT] re-delivers the last
 * intent, but a redelivered intent that was already consumed is ignored to avoid corrupting a
 * finished WAV.
 */
class RecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "reelvoice_recording"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.reelvoice.action.STOP_RECORDING"

        const val EXTRA_ORIGINAL_PATH = "original_path"
        const val EXTRA_CLEANED_PATH = "cleaned_path"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_PRESET = "preset"
        const val EXTRA_REDELIVERED = "redelivered"

        /** Emitted exactly once per recording session when it finishes (success or failure). */
        data class RecordingResult(
            val originalFile: File?,
            val cleanedFile: File?,
            val timestamp: String,
            val success: Boolean,
        )

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _results = MutableSharedFlow<RecordingResult>(replay = 1, extraBufferCapacity = 8)
        val results: SharedFlow<RecordingResult> = _results.asSharedFlow()

        /** Timestamp of the last result consumed by the UI; prevents stale replay re-showing. */
        @Volatile
        var lastHandledTimestamp: String? = null
            private set

        /** Mark a result as consumed so other collectors / replays ignore it. */
        fun markResultHandled(timestamp: String) {
            lastHandledTimestamp = timestamp
        }

        /** Last accepted intent extras; guards against re-recording on redelivery. */
        @Volatile
        private var consumedIntent: Intent? = null

        fun start(context: Context, originalFile: File, cleanedFile: File, timestamp: String, preset: CleaningPreset): Boolean {
            val intent = Intent(context, RecordingService::class.java).apply {
                putExtra(EXTRA_ORIGINAL_PATH, originalFile.absolutePath)
                putExtra(EXTRA_CLEANED_PATH, cleanedFile.absolutePath)
                putExtra(EXTRA_TIMESTAMP, timestamp)
                putExtra(EXTRA_PRESET, preset.name)
            }
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "startForegroundService failed", e)
                false
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, RecordingService::class.java).setAction(ACTION_STOP)
            )
        }

        private fun emitResult(result: RecordingResult) {
            lastHandledTimestamp = null  // not yet consumed by any activity
            _results.tryEmit(result)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var audioProcessor: AudioProcessor
    private var audioRecorder: AudioRecorder? = null
    private var sessionTimestamp: String = ""

    override fun onCreate() {
        super.onCreate()
        audioProcessor = AudioProcessor(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stop request (from notification action or UI).
        if (intent?.action == ACTION_STOP) {
            stopRecordingAndSelf()
            return START_NOT_STICKY
        }

        // START_REDELIVER_INTENT after process death: only act if this exact intent was never
        // consumed, otherwise we would re-record over completed WAV files.
        if (intent?.getBooleanExtra(EXTRA_REDELIVERED, false) == true &&
            intent.filterEquals(consumedIntent)
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (_isRunning.value) {
            // Already recording a session; ignore a second start request.
            return START_STICKY
        }

        val originalPath = intent?.getStringExtra(EXTRA_ORIGINAL_PATH)
        val cleanedPath = intent?.getStringExtra(EXTRA_CLEANED_PATH)
        val timestamp = intent?.getStringExtra(EXTRA_TIMESTAMP)
        val presetName = intent?.getStringExtra(EXTRA_PRESET)
        if (originalPath == null || cleanedPath == null || timestamp == null || presetName == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()

        if (!audioProcessor.initialize()) {
            emitResult(RecordingResult(null, null, timestamp, success = false))
            stopSelf()
            return START_NOT_STICKY
        }
        audioProcessor.cleaningPreset = CleaningPreset.valueOf(presetName).forRecording()

        val originalFile = File(originalPath)
        val cleanedFile = File(cleanedPath)
        sessionTimestamp = timestamp

        val recorder = AudioRecorder(applicationContext)
        audioRecorder = recorder
        recorder.setAudioProcessor(audioProcessor)
        recorder.setCallbacks(
            onData = null,
            onFinished = { origFile, cleanFile ->
                audioProcessor.destroy()
                _isRunning.value = false
                emitResult(RecordingResult(origFile, cleanFile, sessionTimestamp, success = true))
            }
        )

        val started = recorder.startRecording(originalFile, cleanedFile)
        if (!started) {
            audioProcessor.destroy()
            audioRecorder = null
            _isRunning.value = false
            emitResult(RecordingResult(originalFile, cleanedFile, timestamp, success = false))
            stopSelf()
            return START_NOT_STICKY
        }

        _isRunning.value = true
        consumedIntent = intent
        return START_REDELIVER_INTENT
    }

    private fun startInForeground() {
        createChannel()
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.recording_notification_text))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.recording_notification_stop),
                stopIntent
            )
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.recording_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopRecordingAndSelf() {
        // stopRecording() joins the worker thread (<=10s) before releasing AudioRecord/effects,
        // which triggers onFinished -> emitResult, then we stop the service.
        audioRecorder?.stopRecording()
        audioRecorder = null
        _isRunning.value = false
        stopSelf()
    }

    override fun onDestroy() {
        // Safety net: if the system destroyed us without ACTION_STOP (e.g. timeout), make sure
        // every resource is released exactly once. stopRecording() is idempotent.
        audioRecorder?.let { recorder ->
            audioProcessor.destroy()
            try {
                recorder.stopRecording()
            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "onDestroy stopRecording failed", e)
            }
        }
        audioRecorder = null
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
