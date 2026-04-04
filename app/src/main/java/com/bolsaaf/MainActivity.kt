package com.bolsaaf

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.bolsaaf.audio.AudioProcessor
import com.bolsaaf.audio.AudioRecorder
import com.bolsaaf.ui.screens.CleanItem
import com.bolsaaf.ui.screens.HomeScreen
import com.bolsaaf.ui.theme.BolSaafTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var audioProcessor: AudioProcessor
    private lateinit var audioRecorder: AudioRecorder
    private var tempRecordingFile: File? = null
    
    private var isRecording by mutableStateOf(false)
    private var freeMinutesLeft by mutableIntStateOf(8)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processAudioFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioProcessor = AudioProcessor(this)
        audioRecorder = AudioRecorder(this)

        checkPermissions()

        setContent {
            BolSaafTheme {
                HomeScreen(
                    isRecording = isRecording,
                    onStartRecording = { startRecording() },
                    onStopRecording = { stopRecording() },
                    onUploadFile = { pickAudioLauncher.launch("audio/*") },
                    onGoToHistory = { },
                    recentCleans = getRecentCleans(),
                    freeMinutesLeft = freeMinutesLeft
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioProcessor.destroy()
        audioRecorder.stopRecording()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val needPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(needPermissions.toTypedArray())
        }
    }

    private fun startRecording() {
        if (!audioProcessor.initialize()) return
        
        tempRecordingFile = File(cacheDir, "temp_recording_${System.currentTimeMillis()}.wav")
        
        audioRecorder.setCallbacks(
            onData = { buffer -> audioProcessor.processFrame(buffer) },
            onFinished = { 
                audioProcessor.destroy()
                isRecording = false
            }
        )
        
        audioRecorder.startRecording(tempRecordingFile!!)
        isRecording = true
    }

    private fun stopRecording() {
        audioRecorder.stopRecording()
        isRecording = false
    }

    private fun processAudioFile(uri: Uri) {
        if (!audioProcessor.initialize()) return
        
        val outputFile = File(cacheDir, "cleaned_${System.currentTimeMillis()}.wav")
        
        Thread {
            val success = audioProcessor.cleanAudioFile(uri, outputFile) { progress -> }
            runOnUiThread {
                audioProcessor.destroy()
            }
        }.start()
    }

    private fun getRecentCleans(): List<CleanItem> {
        return listOf(
            CleanItem("Lecture_2026.mp3", "98% noise removed", "2h ago"),
            CleanItem("Reel_Traffic.mp4", "Cleaned just now", "5m ago"),
            CleanItem("Podcast_Ep1.wav", "Studio quality", "1d ago")
        )
    }
}
