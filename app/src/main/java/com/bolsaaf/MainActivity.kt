package com.bolsaaf

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bolsaaf.ui.screens.AudioPair
import com.bolsaaf.ui.screens.HistoryScreen
import com.bolsaaf.ui.screens.LiveScreen
import com.bolsaaf.ui.screens.SaveInfo
import com.bolsaaf.audio.AudioProcessor
import com.bolsaaf.audio.CleaningPreset
import com.bolsaaf.audio.AudioRecorder
import com.bolsaaf.ui.screens.CleanItem
import com.bolsaaf.ui.screens.HomeScreen
import com.bolsaaf.ui.theme.BolSaafTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var audioProcessor: AudioProcessor
    private lateinit var audioRecorder: AudioRecorder
    private var tempRecordingFile: File? = null
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingFile by mutableStateOf<String?>(null)
    private var isPlaying by mutableStateOf(false)
    private var isRecording by mutableStateOf(false)
    private var isCleaning by mutableStateOf(false)
    private var isUploading by mutableStateOf(false)
    private var uploadProgress by mutableIntStateOf(0)
    private var selectedFileUri: Uri? = null
    private var showCleanButton by mutableStateOf(false)
    private var currentProcessedPair: AudioPair? = null
    private var showSuccess by mutableStateOf(false)
    private var lastSaveInfo by mutableStateOf<SaveInfo?>(null)
    private var cleaningPreset by mutableStateOf(CleaningPreset.NORMAL)
    private var freeMinutesLeft by mutableIntStateOf(8)

    // Store audio pairs for comparison (timestamp -> AudioPair)
    private var audioPairsList = mutableStateListOf<AudioPair>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            selectedFileUri = it
            isUploading = true
            uploadProgress = 0
            // Start upload simulation with progress
            simulateUploadProgress(it)
        }
    }

    // Navigation state
    private var selectedTab by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioProcessor = AudioProcessor(this)
        audioRecorder = AudioRecorder(this)

        checkPermissions()

        setContent {
            BolSaafTheme {
                val recDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
                when (selectedTab) {
                    0 -> HomeScreen(
                        recordingsDir = recDir,
                        isRecording = isRecording,
                        isCleaning = isCleaning,
                        isUploading = isUploading,
                        uploadProgress = uploadProgress,
                        showSuccess = showSuccess,
                        lastSaveInfo = lastSaveInfo,
                        showCleanButton = showCleanButton,
                        selectedFileName = selectedFileUri?.let { getFileName(it) },
                        selectedTab = selectedTab,
                        cleaningPreset = cleaningPreset,
                        onCleaningPresetChange = { cleaningPreset = it },
                        onStartRecording = { startRecording() },
                        onStopRecording = { stopRecording() },
                        onUploadFile = { pickAudioLauncher.launch("audio/*") },
                        onCleanFile = { startCleaningProcess() },
                        onCancelUpload = { cancelUpload() },
                        onGoToHistory = { selectedTab = 2 },
                        onTabSelected = { tab -> selectedTab = tab },
                        audioPairs = getAudioPairs(),
                        currentProcessedPair = currentProcessedPair,
                        freeMinutesLeft = freeMinutesLeft,
                        currentlyPlaying = currentlyPlayingFile,
                        onPlayFile = { fileName -> playAudioFile(fileName) },
                        onStopFile = { stopPlayback() },
                        onRemovePair = { timestamp -> removeAudioPair(timestamp) },
                        onShareFile = { fileName -> shareFile(fileName) },
                        onDownloadFile = { fileName -> downloadFile(fileName) }
                    )
                    1 -> LiveScreen(
                        isRecording = isRecording,
                        audioLevel = 0.7f,
                        selectedTab = selectedTab,
                        freeMinutesLeft = freeMinutesLeft,
                        audioPairs = getAudioPairs(),
                        currentlyPlaying = currentlyPlayingFile,
                        cleaningPreset = cleaningPreset,
                        onCleaningPresetChange = { cleaningPreset = it },
                        onStartRecording = { startRecording() },
                        onStopRecording = { stopRecording() },
                        onTabSelected = { tab -> selectedTab = tab },
                        onPlayFile = { fileName -> playAudioFile(fileName) },
                        onStopFile = { stopPlayback() },
                        onRemovePair = { timestamp -> removeAudioPair(timestamp) },
                        onShareFile = { fileName -> shareFile(fileName) },
                        onDownloadFile = { fileName -> downloadFile(fileName) },
                        onGoBack = { selectedTab = 0 }
                    )
                    2 -> HistoryScreen(
                        recordingsDir = recDir,
                        selectedTab = selectedTab,
                        audioPairs = getAudioPairs(),
                        currentlyPlaying = currentlyPlayingFile,
                        onTabSelected = { tab -> selectedTab = tab },
                        onPlayFile = { fileName -> playAudioFile(fileName) },
                        onStopFile = { stopPlayback() },
                        onRemovePair = { timestamp -> removeAudioPair(timestamp) },
                        onShareFile = { fileName -> shareFile(fileName) },
                        onDownloadFile = { fileName -> downloadFile(fileName) },
                        onBack = { selectedTab = 0 }
                    )
                    else -> HomeScreen(
                        recordingsDir = recDir,
                        isRecording = isRecording,
                        isCleaning = isCleaning,
                        isUploading = isUploading,
                        uploadProgress = uploadProgress,
                        showSuccess = showSuccess,
                        lastSaveInfo = lastSaveInfo,
                        showCleanButton = showCleanButton,
                        selectedFileName = selectedFileUri?.let { getFileName(it) },
                        selectedTab = selectedTab,
                        cleaningPreset = cleaningPreset,
                        onCleaningPresetChange = { cleaningPreset = it },
                        onStartRecording = { startRecording() },
                        onStopRecording = { stopRecording() },
                        onUploadFile = { pickAudioLauncher.launch("audio/*") },
                        onCleanFile = { startCleaningProcess() },
                        onCancelUpload = { cancelUpload() },
                        onGoToHistory = { selectedTab = 2 },
                        onTabSelected = { tab -> selectedTab = tab },
                        audioPairs = getAudioPairs(),
                        currentProcessedPair = currentProcessedPair,
                        freeMinutesLeft = freeMinutesLeft,
                        currentlyPlaying = currentlyPlayingFile,
                        onPlayFile = { fileName -> playAudioFile(fileName) },
                        onStopFile = { stopPlayback() },
                        onRemovePair = { timestamp -> removeAudioPair(timestamp) },
                        onShareFile = { fileName -> shareFile(fileName) },
                        onDownloadFile = { fileName -> downloadFile(fileName) }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.stopRecording()
        audioProcessor.destroy()
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
        if (!audioProcessor.initialize()) {
            Toast.makeText(this, "Failed to initialize audio processor", Toast.LENGTH_SHORT).show()
            return
        }
        audioProcessor.cleaningPreset = cleaningPreset
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        // Set audio processor for RNNoise
        audioRecorder.setAudioProcessor(audioProcessor)
        
        // Create output directory if not exists
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val originalFile = File(outputDir, "original_$timestamp.wav")
        val cleanedFile = File(outputDir, "cleaned_$timestamp.wav")
        tempRecordingFile = originalFile
        
        audioRecorder.setCallbacks(
            onData = { buffer ->
                // Callback for live audio visualization if needed
            },
            onFinished = { origFile, cleanFile ->
                audioProcessor.destroy()
                isRecording = false

                val durationSec = wavDurationSec(cleanFile)
                runOnUiThread {
                    showSuccess = true
                    lastSaveInfo = SaveInfo(cleanFile.name, durationSec, timestamp)
                    addAudioPair(timestamp, origFile, cleanFile, isRecording = true)
                    Toast.makeText(this, "Saved · ${"%.1f".format(durationSec)}s · ${cleanFile.name}", Toast.LENGTH_LONG).show()
                    window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }

                Thread {
                    Thread.sleep(2800)
                    runOnUiThread {
                        showSuccess = false
                        lastSaveInfo = null
                    }
                }.start()
            }
        )
        
        val started = audioRecorder.startRecording(originalFile, cleanedFile)
        if (started) {
            isRecording = true
            Toast.makeText(this, "Recording… speak normally", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            audioProcessor.destroy()
        }
    }

    private fun saveWavFile(pcmData: ByteArray, outputFile: File, sampleRate: Int, channels: Int) {
        val byteRate = sampleRate * channels * 2
        val totalDataLen = pcmData.size + 36
        val totalAudioLen = pcmData.size

        java.io.FileOutputStream(outputFile).use { fos ->
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
            fos.write(pcmData)
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToBytes(value: Short): ByteArray {
        return java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }

    private fun stopRecording() {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        audioRecorder.stopRecording()
        isRecording = false
        Toast.makeText(this, "Finishing & saving…", Toast.LENGTH_SHORT).show()
    }

    private fun playAudioFile(fileName: String) {
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        val file = File(outputDir, fileName)

        android.util.Log.d("BolSaaf", "Play: ${file.absolutePath}, exists=${file.exists()}, len=${file.length()}")

        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Second tap on same row = stop (AudioTrack thread used to race; MediaPlayer stops cleanly)
        if (currentlyPlayingFile == fileName) {
            stopPlayback()
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
            return
        }

        stopPlayback()

        try {
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener { player ->
                runOnUiThread {
                    if (currentlyPlayingFile == fileName) {
                        currentlyPlayingFile = null
                        isPlaying = false
                    }
                }
                try {
                    player.release()
                } catch (_: Exception) {
                }
                if (mediaPlayer === player) {
                    mediaPlayer = null
                }
            }
            mp.setOnErrorListener { player, _, _ ->
                runOnUiThread {
                    Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show()
                    currentlyPlayingFile = null
                    isPlaying = false
                }
                try {
                    player.release()
                } catch (_: Exception) {
                }
                if (mediaPlayer === player) {
                    mediaPlayer = null
                }
                true
            }
            mp.prepare()
            mp.start()
            currentlyPlayingFile = fileName
            isPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Cannot play: ${e.message}", Toast.LENGTH_SHORT).show()
            currentlyPlayingFile = null
            isPlaying = false
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        currentlyPlayingFile = null
        isPlaying = false
    }

    override fun onPause() {
        super.onPause()
        stopPlayback()
    }

    private fun processAudioFile(uri: Uri) {
        if (!audioProcessor.initialize()) {
            Toast.makeText(this, "Failed to initialize audio processor", Toast.LENGTH_SHORT).show()
            return
        }
        audioProcessor.cleaningPreset = cleaningPreset
        
        // Create output directory if not exists
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val originalFile = File(outputDir, "uploaded_$timestamp${guessExtensionForUri(uri)}")
        val cleanedFile = File(outputDir, "cleaned_$timestamp.wav")
        
        Thread {
            try {
                // First save the original file
                saveOriginalFromUri(uri, originalFile)
                
                isCleaning = true
                val success = audioProcessor.cleanAudioFile(uri, cleanedFile) { progress ->
                    runOnUiThread {
                        // Show progress if needed
                    }
                }
                runOnUiThread {
                    audioProcessor.destroy()
                    isCleaning = false
                    isUploading = false
                    if (success) {
                        showSuccess = true
                        lastSaveInfo = SaveInfo(cleanedFile.name, wavDurationSec(cleanedFile), timestamp)
                        addAudioPair(timestamp, originalFile, cleanedFile, isRecording = false)
                        Toast.makeText(this, "Cleaned · ${cleanedFile.name}", Toast.LENGTH_LONG).show()
                        Thread {
                            Thread.sleep(2800)
                            runOnUiThread {
                                showSuccess = false
                                lastSaveInfo = null
                            }
                        }.start()
                    } else {
                        Toast.makeText(this, "Failed to process file", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    isCleaning = false
                    audioProcessor.destroy()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun saveOriginalFromUri(uri: Uri, outputFile: File) {
        contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    /** Real extension so MediaPlayer decodes m4a/mp3 correctly (was always .wav). */
    private fun guessExtensionForUri(uri: Uri): String {
        contentResolver.getType(uri)?.let { mime ->
            when {
                mime.contains("wav", ignoreCase = true) -> return ".wav"
                mime.contains("mpeg", ignoreCase = true) || mime.contains("mp3", ignoreCase = true) -> return ".mp3"
                mime.contains("mp4", ignoreCase = true) || mime.contains("m4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> return ".m4a"
                mime.contains("ogg", ignoreCase = true) -> return ".ogg"
                mime.contains("flac", ignoreCase = true) -> return ".flac"
                else -> { }
            }
        }
        val name = getFileName(uri)
        val dot = name.lastIndexOf('.')
        if (dot >= 0 && dot < name.length - 1) {
            return name.substring(dot).lowercase(Locale.getDefault())
        }
        return ".bin"
    }

    private fun wavDurationSec(wav: File): Float {
        if (!wav.exists() || wav.length() < 1000) return 0f
        val pcmBytes = wav.length() - 44
        if (pcmBytes <= 0) return 0f
        return (pcmBytes / 2) / 48000f
    }

    // Add audio pair (Original + Cleaned together)
    private fun addAudioPair(timestamp: String, originalFile: File, cleanedFile: File, isRecording: Boolean = true) {
        val time = getCurrentTimeFormatted()
        val pair = AudioPair(
            timestamp = timestamp,
            time = time,
            originalFile = originalFile.name,
            cleanedFile = cleanedFile.name,
            isRecording = isRecording,
            durationSec = wavDurationSec(cleanedFile)
        )
        // Remove any existing pair with same timestamp
        audioPairsList.removeAll { it.timestamp == timestamp }
        // Add new pair at the beginning
        audioPairsList.add(0, pair)
        // Keep only last 10 pairs
        while (audioPairsList.size > 10) {
            audioPairsList.removeAt(audioPairsList.size - 1)
        }
    }

    // Remove audio pair by timestamp
    private fun removeAudioPair(timestamp: String) {
        val pairToRemove = audioPairsList.find { pair -> pair.timestamp == timestamp }
        pairToRemove?.let { pair ->
            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
            // Delete files
            File(outputDir, pair.originalFile).delete()
            File(outputDir, pair.cleanedFile).delete()
            // Remove from list
            audioPairsList.remove(pair)
            Toast.makeText(this, "Files deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAudioPairs(): List<AudioPair> {
        return audioPairsList
    }

    private fun getCurrentTimeFormatted(): String {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(calendar.time)
    }
    
    // Get file name from URI
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Unknown file"
    }
    
    // Simulate upload progress
    private fun simulateUploadProgress(uri: Uri) {
        Thread {
            try {
                // Simulate upload progress (0-100%)
                for (i in 0..100 step 5) {
                    runOnUiThread { uploadProgress = i }
                    Thread.sleep(50) // 50ms per 5% = ~1 second total
                }
                runOnUiThread {
                    isUploading = false
                    showCleanButton = true
                    Toast.makeText(this, "File uploaded! Tap Clean to process", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    isUploading = false
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    // Cancel upload
    private fun cancelUpload() {
        selectedFileUri = null
        isUploading = false
        uploadProgress = 0
        showCleanButton = false
        Toast.makeText(this, "Upload cancelled", Toast.LENGTH_SHORT).show()
    }
    
    // Start cleaning process
    private fun startCleaningProcess() {
        selectedFileUri?.let { uri ->
            showCleanButton = false
            processAudioFileWithResult(uri)
        }
    }
    
    // Process audio file with result tracking
    private fun processAudioFileWithResult(uri: Uri) {
        if (!audioProcessor.initialize()) {
            Toast.makeText(this, "Failed to initialize audio processor", Toast.LENGTH_SHORT).show()
            return
        }
        audioProcessor.cleaningPreset = cleaningPreset
        
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val originalFile = File(outputDir, "uploaded_$timestamp${guessExtensionForUri(uri)}")
        val cleanedFile = File(outputDir, "cleaned_$timestamp.wav")
        
        Thread {
            try {
                saveOriginalFromUri(uri, originalFile)
                
                isCleaning = true
                val success = audioProcessor.cleanAudioFile(uri, cleanedFile) { progress ->
                    runOnUiThread {
                        // Update progress if needed
                    }
                }
                
                runOnUiThread {
                    audioProcessor.destroy()
                    isCleaning = false
                    selectedFileUri = null
                    
                    if (success) {
                        val pair = AudioPair(
                            timestamp = timestamp,
                            time = getCurrentTimeFormatted(),
                            originalFile = originalFile.name,
                            cleanedFile = cleanedFile.name,
                            isRecording = false,
                            durationSec = wavDurationSec(cleanedFile)
                        )
                        currentProcessedPair = pair
                        addAudioPair(timestamp, originalFile, cleanedFile, isRecording = false)
                        showSuccess = true
                        lastSaveInfo = SaveInfo(cleanedFile.name, wavDurationSec(cleanedFile), timestamp)
                        Toast.makeText(this, "Cleaned · ${"%.1f".format(wavDurationSec(cleanedFile))}s", Toast.LENGTH_LONG).show()
                        window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        Thread {
                            Thread.sleep(2800)
                            runOnUiThread {
                                showSuccess = false
                                lastSaveInfo = null
                            }
                        }.start()
                    } else {
                        Toast.makeText(this, "Failed to process file", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    isCleaning = false
                    audioProcessor.destroy()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    // Share file
    private fun shareFile(fileName: String) {
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        val file = File(outputDir, fileName)
        
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "audio/wav"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Cleaned Audio")
            putExtra(android.content.Intent.EXTRA_TEXT, "Here is my cleaned audio file!")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Audio"))
    }
    
    // Download file (copy to Downloads)
    private fun downloadFile(fileName: String) {
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        val sourceFile = File(outputDir, fileName)
        
        if (!sourceFile.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, fileName)
            
            sourceFile.copyTo(destFile, overwrite = true)
            
            Toast.makeText(this, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to download: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
