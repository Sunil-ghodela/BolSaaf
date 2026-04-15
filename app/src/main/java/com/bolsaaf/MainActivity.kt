package com.bolsaaf

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.bolsaaf.ui.VibeUi
import com.bolsaaf.ui.screens.AudioPair
import com.bolsaaf.ui.screens.HistoryScreen
import com.bolsaaf.ui.screens.LiveScreen
import com.bolsaaf.ui.screens.SaveInfo
import com.bolsaaf.audio.AudioProcessor
import com.bolsaaf.audio.AuthApi
import com.bolsaaf.audio.CleaningPreset
import com.bolsaaf.audio.AudioRecorder
import com.bolsaaf.audio.VoiceApiPhase2Client
import com.bolsaaf.audio.AdaptiveAudioAnalyzer
import com.bolsaaf.audio.FeedbackAdaptiveMemory
import com.bolsaaf.audio.pcm16LeToShortArray
import com.bolsaaf.audio.ProcessingQualityGuard
import com.bolsaaf.audio.VoiceBackground
import com.bolsaaf.audio.VoiceCleaningApi
import com.bolsaaf.ui.screens.CleanItem
import com.bolsaaf.ui.screens.FastLibTestScreen
import com.bolsaaf.ui.screens.HomeScreen
import com.bolsaaf.ui.screens.ProfileScreen
import com.bolsaaf.ui.screens.SettingsDialog
import com.bolsaaf.ui.theme.BolSaafTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        const val FREE_QUOTA_MINUTES = 20
        const val QUOTA_WARN_THRESHOLD = 3
        const val SUPPORT_EMAIL = "ss.sunil9255@gmail.com"
        // FastLib caps (must stay ≤ server nginx `voice_upload` client_max_body_size = 50M).
        const val FASTLIB_MAX_VIDEO_BYTES: Long = 50L * 1024 * 1024  // 50 MB
        const val FASTLIB_MAX_AUDIO_BYTES: Long = 25L * 1024 * 1024  // 25 MB soft cap for audio
        // Rate limits — informational (enforced server-side via DRF ScopedRateThrottle).
        const val FASTLIB_RATE_AUDIO_PER_MIN = 15
        const val FASTLIB_RATE_VIDEO_PER_MIN = 5
    }

    private lateinit var audioProcessor: AudioProcessor
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var voiceApi: VoiceCleaningApi
    private lateinit var voiceApiPhase2: VoiceApiPhase2Client
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
    private var processingFlow by mutableStateOf(ProcessingFlow.REEL_MODE)
    private var freeMinutesLeft by mutableIntStateOf(FREE_QUOTA_MINUTES)
    /** Order matches Home flow chips: Reel first (product default). */
    private enum class ProcessingFlow {
        REEL_MODE, CLEAN, ADD_BACKGROUND, VIDEO_PROCESS
    }

    private var serverAvailableModes by mutableStateOf<Set<String>>(emptySet())
    private var voiceBackgrounds by mutableStateOf<List<VoiceBackground>>(emptyList())
    private var selectedBackgroundIndex by mutableIntStateOf(0)
    private var showVibeSheet by mutableStateOf(false)
    private var showRecordFormatSheet by mutableStateOf(false)
    private var showVideoRecorder by mutableStateOf(false)
    private var liveFormat by mutableStateOf(RecordFormat.AUDIO)

    enum class RecordFormat { AUDIO, VIDEO }
    private var bgMixVolume by mutableStateOf(0.15f)
    private var lastAdaptiveProfile by mutableStateOf<AdaptiveAudioAnalyzer.Profile?>(null)
    private var adaptiveAnalysisLoading by mutableStateOf(false)
    private var phase2StageName by mutableStateOf<String?>(null)
    private var phase2OverallProgress by mutableIntStateOf(0)
    private var latestReelVariantFiles by mutableStateOf<Map<String, String>>(emptyMap())
    private var showSettingsDialog by mutableStateOf(false)
    private var isUserLoggedIn by mutableStateOf(false)
    private var userEmail by mutableStateOf<String?>(null)
    private var userDisplayName by mutableStateOf("You")
    private var userHandle by mutableStateOf("@bolsaaf")
    private var isProMember by mutableStateOf(false)
    private val authApi = AuthApi()
    private var fastLibInputUri by mutableStateOf<Uri?>(null)
    private var fastLibInputIsVideo by mutableStateOf(false)
    private var fastLibInputLabel by mutableStateOf<String?>(null)
    private var fastLibIsCleaning by mutableStateOf(false)
    private var fastLibOutputFileName by mutableStateOf<String?>(null)
    private var fastLibStage by mutableStateOf<String?>(null) // uploading | processing | downloading
    private var fastLibProgress by mutableIntStateOf(0)       // 0..100 during downloading
    private val fastLibOutputs = mutableStateListOf<String>() // most-recent-first filenames

    // Store audio pairs for comparison (timestamp -> AudioPair)
    private var audioPairsList = mutableStateListOf<AudioPair>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { handlePickedMediaUri(it) } }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { handlePickedMediaUri(it) } }

    private val pickFastAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (rejectIfOversized(it, isVideo = false)) return@let
            fastLibInputUri = it
            fastLibInputIsVideo = false
            fastLibInputLabel = getFileName(it)
            fastLibOutputFileName = null
        }
    }

    private val pickFastVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (rejectIfOversized(it, isVideo = true)) return@let
            fastLibInputUri = it
            fastLibInputIsVideo = true
            fastLibInputLabel = getFileName(it)
            fastLibOutputFileName = null
        }
    }

    /**
     * Returns true (and shows a toast) if the picked URI exceeds the FastLib upload cap.
     * Cap mirrors server nginx `voice_upload` location: 50 MB (audio or video).
     * Audio tier is soft-capped at 25 MB because most voice clips fit easily
     * and bigger audio is usually a mis-pick.
     */
    private fun rejectIfOversized(uri: Uri, isVideo: Boolean): Boolean {
        val size = uriContentSize(uri) ?: return false // unknown size — let server decide
        val maxBytes = if (isVideo) FASTLIB_MAX_VIDEO_BYTES else FASTLIB_MAX_AUDIO_BYTES
        return if (size > maxBytes) {
            val limitMb = maxBytes / (1024 * 1024)
            val gotMb = "%.1f".format(size / 1024.0 / 1024.0)
            Toast.makeText(
                this,
                "File too large: ${gotMb} MB. Max ${limitMb} MB for " +
                    (if (isVideo) "video" else "audio") + ".",
                Toast.LENGTH_LONG
            ).show()
            true
        } else false
    }

    private fun uriContentSize(uri: Uri): Long? {
        return try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun handlePickedMediaUri(uri: Uri) {
        selectedFileUri = uri
        lastAdaptiveProfile = null
        adaptiveAnalysisLoading = false
        isUploading = true
        uploadProgress = 0
        simulateUploadProgress(uri)
    }

    private fun openUploadPicker() {
        when (processingFlow) {
            ProcessingFlow.VIDEO_PROCESS -> pickVideoLauncher.launch("video/*")
            else -> pickAudioLauncher.launch("audio/*")
        }
    }

    /**
     * Atomically switch to VIDEO_PROCESS and open the video picker.
     * Avoids any read-after-write timing ambiguity when callers sequentially
     * change mode then open the picker on the next line — the previous
     * "Video Clean" tap sometimes opened the audio picker because
     * openUploadPicker() reads processingFlow and the mutation hadn't
     * propagated yet. This is the safe single-call path.
     */
    private fun openVideoPickerDirect() {
        processingFlow = ProcessingFlow.VIDEO_PROCESS
        pickVideoLauncher.launch("video/*")
    }

    private fun openFastAudioPicker() {
        pickFastAudioLauncher.launch("audio/*")
    }

    private fun openFastVideoPicker() {
        pickFastVideoLauncher.launch("video/*")
    }

    private fun milderCloudMode(current: String, available: Set<String>): String? {
        val order = listOf("pro", "studio", "standard", "basic")
        val idx = order.indexOf(current)
        if (idx < 0 || idx >= order.lastIndex) return null
        for (k in idx + 1 until order.size) {
            val m = order[k]
            if (available.isEmpty() || available.contains(m)) return m
        }
        return null
    }

    // Navigation state
    private var selectedTab by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioProcessor = AudioProcessor(this)
        audioRecorder = AudioRecorder(this)
        voiceApi = VoiceCleaningApi()
        voiceApiPhase2 = VoiceApiPhase2Client()
        freeMinutesLeft = loadFreeMinutes()
        restoreAuthSession()
        refreshVoiceApiCapabilities()

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
                        onDismissSuccess = { showSuccess = false },
                        lastSaveInfo = lastSaveInfo,
                        showCleanButton = showCleanButton,
                        selectedFileName = selectedFileUri?.let { getFileName(it) },
                        selectedTab = selectedTab,
                        cleaningPreset = cleaningPreset,
                        modeAvailabilityNote = cloudModeAvailabilityNote(),
                        processingModes = listOf(
                            "Reel ★",
                            "Quick",
                            "BG",
                            "Video"
                        ),
                        selectedProcessingModeIndex = processingFlow.ordinal,
                        onProcessingModeChange = {
                            processingFlow = ProcessingFlow.values()[it]
                        },
                        onCleaningPresetChange = { cleaningPreset = it },
                        // On Home, tapping Start Recording opens the audio/video chooser
                        // sheet; the sheet then either calls startRecording() (audio)
                        // or opens the CameraX video overlay.
                        onStartRecording = {
                            if (isRecording) stopRecording() else showRecordFormatSheet = true
                        },
                        onStopRecording = { stopRecording() },
                        onUploadFile = { openUploadPicker() },
                        onVideoUpload = { openVideoPickerDirect() },
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
                        onDownloadFile = { fileName -> downloadFile(fileName) },
                        onSubmitFeedback = { pair, clearVoice, issueType, issueTs, notes ->
                            submitUserFeedback(pair, clearVoice, issueType, issueTs, notes)
                        },
                        showBackgroundControls = processingFlow == ProcessingFlow.ADD_BACKGROUND ||
                            processingFlow == ProcessingFlow.REEL_MODE,
                        backgroundLabels = voiceBackgrounds.map { b ->
                            VibeUi.displayLabelForBackgroundId(b.id, b.label.ifBlank { b.id })
                        },
                        selectedBackgroundIndex = selectedBackgroundIndex,
                        onBackgroundIndexChange = { selectedBackgroundIndex = it },
                        bgMixVolume = bgMixVolume,
                        onBgMixVolumeChange = { bgMixVolume = it },
                        processPrimaryButtonLabel = processPrimaryButtonLabel(),
                        processHelperText = processHelperText(),
                        processingDialogTitle = processingDialogTitle(),
                        processingDialogSubtitle = processingDialogSubtitle(),
                        adaptiveProfile = lastAdaptiveProfile,
                        adaptiveAnalysisLoading = adaptiveAnalysisLoading,
                        onApplyAdaptivePreset = {
                            lastAdaptiveProfile?.let { cleaningPreset = it.suggestedCleaningPreset }
                        },
                        onProminentReelClick = { processingFlow = ProcessingFlow.REEL_MODE },
                        onOpenSettings = { showSettingsDialog() },
                        reelVariantFiles = latestReelVariantFiles,
                        onPlayReelVariant = { fileName -> playAudioFile(fileName) },
                        onShareReelVariant = { fileName -> shareFile(fileName) },
                        onDownloadReelVariant = { fileName -> downloadFile(fileName) }
                    )
                    1 -> LiveScreen(
                        isRecording = isRecording,
                        audioLevel = 0.7f,
                        selectedTab = selectedTab,
                        freeMinutesLeft = freeMinutesLeft,
                        audioPairs = getAudioPairs(),
                        currentlyPlaying = currentlyPlayingFile,
                        cleaningPreset = cleaningPreset,
                        modeAvailabilityNote = cloudModeAvailabilityNote(),
                        isVideoMode = liveFormat == RecordFormat.VIDEO,
                        onToggleFormat = { isVideo ->
                            liveFormat = if (isVideo) RecordFormat.VIDEO else RecordFormat.AUDIO
                        },
                        onStartVideoRecording = {
                            requestCameraThen { showVideoRecorder = true }
                        },
                        onCleaningPresetChange = { cleaningPreset = it },
                        onStartRecording = { startRecording() },
                        onStopRecording = { stopRecording() },
                        onTabSelected = { tab -> selectedTab = tab },
                        onPlayFile = { fileName -> playAudioFile(fileName) },
                        onStopFile = { stopPlayback() },
                        onRemovePair = { timestamp -> removeAudioPair(timestamp) },
                        onShareFile = { fileName -> shareFile(fileName) },
                        onDownloadFile = { fileName -> downloadFile(fileName) },
                        onSubmitFeedback = { pair, clearVoice, issueType, issueTs, notes ->
                            submitUserFeedback(pair, clearVoice, issueType, issueTs, notes)
                        },
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
                        onSubmitFeedback = { pair, clearVoice, issueType, issueTs, notes ->
                            submitUserFeedback(pair, clearVoice, issueType, issueTs, notes)
                        },
                        onBack = { selectedTab = 0 }
                    )
                    3 -> ProfileScreen(
                        selectedTab = selectedTab,
                        onTabSelected = { tab -> selectedTab = tab },
                        cleaningPreset = cleaningPreset,
                        freeMinutesLeft = freeMinutesLeft,
                        freeQuotaMinutes = FREE_QUOTA_MINUTES,
                        completedCleans = getAudioPairs().size,
                        totalProcessedMinutes = totalProcessedMinutes(),
                        dayStreak = readProfileStreak(),
                        displayName = userDisplayName,
                        userHandle = userHandle,
                        userEmail = userEmail,
                        isLoggedIn = isUserLoggedIn,
                        showProMemberBadge = isProMember,
                        onUpgrade = {
                            Toast.makeText(this, "Upgrade flow coming soon — ping support from Settings.", Toast.LENGTH_SHORT).show()
                        },
                        onOpenSettings = { showSettingsDialog() },
                        onLogin = { email, password -> handleLogin(email, password) },
                        onRegister = { email, password, name -> handleRegister(email, password, name) },
                        onPasswordReset = { email -> handlePasswordReset(email) },
                        onLogout = { handleLogout() }
                    )
                    4 -> FastLibTestScreen(
                        selectedTab = selectedTab,
                        recordingsDir = recDir,
                        selectedFileLabel = fastLibInputLabel,
                        isVideoInput = fastLibInputIsVideo,
                        isCleaning = fastLibIsCleaning,
                        stage = fastLibStage,
                        progressPct = fastLibProgress,
                        outputs = fastLibOutputs.toList(),
                        maxAudioMb = (FASTLIB_MAX_AUDIO_BYTES / 1024 / 1024).toInt(),
                        maxVideoMb = (FASTLIB_MAX_VIDEO_BYTES / 1024 / 1024).toInt(),
                        onUploadAudio = { openFastAudioPicker() },
                        onUploadVideo = { openFastVideoPicker() },
                        onStartCleaning = { startFastLibTestCleaning() },
                        onPlayOutput = { name -> playAudioFile(name) },
                        onShareOutput = { name -> shareFile(name) },
                        onSaveOutput = { name -> downloadFile(name) },
                        onTabSelected = { tab -> selectedTab = tab }
                    )
                    else -> HomeScreen(
                        recordingsDir = recDir,
                        isRecording = isRecording,
                        isCleaning = isCleaning,
                        isUploading = isUploading,
                        uploadProgress = uploadProgress,
                        showSuccess = showSuccess,
                        onDismissSuccess = { showSuccess = false },
                        lastSaveInfo = lastSaveInfo,
                        showCleanButton = showCleanButton,
                        selectedFileName = selectedFileUri?.let { getFileName(it) },
                        selectedTab = selectedTab,
                        cleaningPreset = cleaningPreset,
                        modeAvailabilityNote = cloudModeAvailabilityNote(),
                        processingModes = listOf(
                            "Reel ★",
                            "Quick",
                            "BG",
                            "Video"
                        ),
                        selectedProcessingModeIndex = processingFlow.ordinal,
                        onProcessingModeChange = {
                            processingFlow = ProcessingFlow.values()[it]
                        },
                        onCleaningPresetChange = { cleaningPreset = it },
                        onStartRecording = { startRecording() },
                        onStopRecording = { stopRecording() },
                        onUploadFile = { openUploadPicker() },
                        onVideoUpload = { openVideoPickerDirect() },
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
                        onDownloadFile = { fileName -> downloadFile(fileName) },
                        onSubmitFeedback = { pair, clearVoice, issueType, issueTs, notes ->
                            submitUserFeedback(pair, clearVoice, issueType, issueTs, notes)
                        },
                        showBackgroundControls = processingFlow == ProcessingFlow.ADD_BACKGROUND ||
                            processingFlow == ProcessingFlow.REEL_MODE,
                        backgroundLabels = voiceBackgrounds.map { b ->
                            VibeUi.displayLabelForBackgroundId(b.id, b.label.ifBlank { b.id })
                        },
                        selectedBackgroundIndex = selectedBackgroundIndex,
                        onBackgroundIndexChange = { selectedBackgroundIndex = it },
                        bgMixVolume = bgMixVolume,
                        onBgMixVolumeChange = { bgMixVolume = it },
                        processPrimaryButtonLabel = processPrimaryButtonLabel(),
                        processHelperText = processHelperText(),
                        processingDialogTitle = processingDialogTitle(),
                        processingDialogSubtitle = processingDialogSubtitle(),
                        adaptiveProfile = lastAdaptiveProfile,
                        adaptiveAnalysisLoading = adaptiveAnalysisLoading,
                        onApplyAdaptivePreset = {
                            lastAdaptiveProfile?.let { cleaningPreset = it.suggestedCleaningPreset }
                        },
                        onProminentReelClick = { processingFlow = ProcessingFlow.REEL_MODE },
                        onOpenSettings = { showSettingsDialog() },
                        reelVariantFiles = latestReelVariantFiles,
                        onPlayReelVariant = { fileName -> playAudioFile(fileName) },
                        onShareReelVariant = { fileName -> shareFile(fileName) },
                        onDownloadReelVariant = { fileName -> downloadFile(fileName) }
                    )
                }

                // Record format chooser (Audio vs Video)
                if (showRecordFormatSheet) {
                    com.bolsaaf.ui.screens.RecordFormatSheet(
                        onDismiss = { showRecordFormatSheet = false },
                        onPickAudio = {
                            showRecordFormatSheet = false
                            if (isRecording) stopRecording() else startRecording()
                        },
                        onPickVideo = {
                            showRecordFormatSheet = false
                            requestCameraThen { showVideoRecorder = true }
                        }
                    )
                }

                // Full-screen video recorder (CameraX)
                if (showVideoRecorder) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showVideoRecorder = false },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = false
                        )
                    ) {
                        com.bolsaaf.ui.components.VideoRecordOverlay(
                            outputDir = recDir,
                            onRecorded = { file ->
                                showVideoRecorder = false
                                onVideoRecordingFinished(file)
                            },
                            onCancel = { showVideoRecorder = false }
                        )
                    }
                }

                // Settings Dialog
                if (showSettingsDialog) {
                    SettingsDialog(
                        onDismiss = { showSettingsDialog = false },
                        onClearCache = { clearCache() },
                        onAbout = {
                            Toast.makeText(this@MainActivity, "BolSaaf v1.0.1\nAudio & Video Studio", Toast.LENGTH_LONG).show()
                            showSettingsDialog = false
                        },
                        onPrivacyPolicy = {
                            openExternalUrl("https://shadowselfwork.com/voice/privacy")
                            showSettingsDialog = false
                        },
                        onTermsOfService = {
                            openExternalUrl("https://shadowselfwork.com/voice/terms")
                            showSettingsDialog = false
                        },
                        onContactSupport = {
                            openSupportEmail()
                            showSettingsDialog = false
                        }
                    )
                }
            }
        }
    }

    private fun showSettingsDialog() {
        showSettingsDialog = true
    }

    private fun clearCache() {
        try {
            val cacheDir = cacheDir
            val tmpFiles = cacheDir.listFiles()?.filter { it.name.startsWith("tmp") || it.name.startsWith("adaptive") }
            tmpFiles?.forEach { it.delete() }

            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
            val oldFiles = outputDir.listFiles()?.filter {
                it.lastModified() < System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000 // 7 days old
            }
            oldFiles?.forEach { it.delete() }

            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
            showSettingsDialog = false
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to clear cache", Toast.LENGTH_SHORT).show()
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

    /**
     * Called when CameraX finishes writing a recorded MP4 to disk.
     * Adds it to the Recent Cleans list as a video item, so the user can
     * play it back, share, or (next release) run video/process on it.
     */
    private fun onVideoRecordingFinished(file: File) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val original = file // same as "cleaned" for raw recording
        val pair = AudioPair(
            timestamp = ts,
            time = getCurrentTimeFormatted(),
            originalFile = file.name,
            cleanedFile = file.name,
            isRecording = true,
            durationSec = mediaDurationSec(file)
        )
        audioPairsList.removeAll { it.timestamp == ts }
        audioPairsList.add(0, pair)
        while (audioPairsList.size > 10) audioPairsList.removeAt(audioPairsList.lastIndex)
        lastSaveInfo = SaveInfo(file.name, pair.durationSec, ts)
        showSuccess = true
        Toast.makeText(this, "Video saved: ${file.name}", Toast.LENGTH_SHORT).show()
    }

    /** Request CAMERA (+ RECORD_AUDIO if missing) before opening the video recorder. */
    private fun requestCameraThen(onGranted: () -> Unit) {
        val needed = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isEmpty()) {
            onGranted()
            return
        }
        pendingCameraOnGranted = onGranted
        cameraPermissionLauncher.launch(needed.toTypedArray())
    }

    private var pendingCameraOnGranted: (() -> Unit)? = null
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val cameraOk = result[Manifest.permission.CAMERA] ?: (
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
        if (cameraOk) {
            pendingCameraOnGranted?.invoke()
        } else {
            Toast.makeText(this, "Camera permission is required to record video.", Toast.LENGTH_LONG).show()
        }
        pendingCameraOnGranted = null
    }

    // --- Auth session persistence ---

    private fun authPrefs() = getSharedPreferences("bolsaaf_auth", MODE_PRIVATE)
    private fun savedAuthToken(): String? = authPrefs().getString("token", null)

    private fun persistAuthUser(token: String, user: AuthApi.AuthUser) {
        authPrefs().edit()
            .putString("token", token)
            .putString("email", user.email)
            .putString("display_name", user.displayName)
            .putString("handle", user.handle)
            .apply()
        applyAuthUserToUi(user)
    }

    private fun applyAuthUserToUi(user: AuthApi.AuthUser) {
        isUserLoggedIn = true
        userEmail = user.email
        userDisplayName = user.displayName.ifBlank { user.email.substringBefore("@") }
        userHandle = user.handle.ifBlank { "@" + user.email.substringBefore("@").take(10) }
        isProMember = user.isPro
        // Server quota is the source of truth once signed in.
        freeMinutesLeft = user.freeMinutesLeft
        quotaPrefs().edit()
            .putString("period", user.freePeriodYyyyMm ?: SimpleDateFormat("yyyyMM", Locale.US).format(Date()))
            .putInt("left", user.freeMinutesLeft)
            .apply()
    }

    private fun clearAuthSession() {
        authPrefs().edit().clear().apply()
        isUserLoggedIn = false
        userEmail = null
        userDisplayName = "You"
        userHandle = "@bolsaaf"
        isProMember = false
    }

    /** On app start, if we have a stored token, fetch /auth/me/ to refresh quota + pro status. */
    private fun restoreAuthSession() {
        val token = savedAuthToken() ?: return
        Thread {
            try {
                val me = authApi.me(token)
                runOnUiThread { applyAuthUserToUi(me) }
            } catch (e: AuthApi.AuthException) {
                if (e.status == 401 || e.status == 404) {
                    runOnUiThread { clearAuthSession() }
                }
            } catch (_: Exception) {
                // Network failure — keep the cached session silently; next interaction will retry.
            }
        }.start()
    }

    private fun handleLogin(email: String, password: String) {
        Thread {
            try {
                val result = authApi.login(email.trim().lowercase(Locale.US), password)
                runOnUiThread {
                    persistAuthUser(result.token, result.user)
                    Toast.makeText(this, "Welcome back, ${result.user.displayName}!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: AuthApi.AuthException) {
                runOnUiThread { Toast.makeText(this, e.message ?: "Login failed", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Network error — please try again.", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun handleRegister(email: String, password: String, displayName: String?) {
        Thread {
            try {
                val result = authApi.register(email.trim().lowercase(Locale.US), password, displayName?.trim())
                runOnUiThread {
                    persistAuthUser(result.token, result.user)
                    Toast.makeText(this, "Welcome to BolSaaf, ${result.user.displayName}!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: AuthApi.AuthException) {
                runOnUiThread { Toast.makeText(this, e.message ?: "Sign-up failed", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Network error — please try again.", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun handlePasswordReset(email: String) {
        val normalized = email.trim().lowercase(Locale.US)
        if (normalized.isBlank() || "@" !in normalized) {
            Toast.makeText(this, "Enter a valid email first.", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            runCatching { authApi.requestPasswordReset(normalized) }
            runOnUiThread {
                Toast.makeText(
                    this,
                    "If an account exists for $normalized, a reset email is on its way.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    private fun handleLogout() {
        val token = savedAuthToken()
        clearAuthSession()
        Toast.makeText(this, "Signed out.", Toast.LENGTH_SHORT).show()
        if (token != null) Thread { runCatching { authApi.logout(token) } }.start()
    }

    private fun startFastLibTestCleaning() {
        val uri = fastLibInputUri ?: return
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        if (!outputDir.exists()) outputDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val requestedVideo = fastLibInputIsVideo && uriIsLikelyVideoMedia(uri)
        val inputFile = File(outputDir, "fastlib_input_$timestamp${guessExtensionForUri(uri)}")

        Thread {
            try {
                saveOriginalFromUri(uri, inputFile)
                val treatAsVideo = requestedVideo && fileHasVideoTrack(inputFile)
                val outFile = if (treatAsVideo) {
                    File(outputDir, "fastlib_cleaned_$timestamp.mp4")
                } else {
                    File(outputDir, "fastlib_cleaned_$timestamp.wav")
                }
                runOnUiThread {
                    fastLibIsCleaning = true
                    fastLibStage = "uploading"
                    fastLibProgress = 0
                    if (fastLibInputIsVideo && !treatAsVideo) {
                        Toast.makeText(this, "No video track detected. Running audio clean.", Toast.LENGTH_SHORT).show()
                    }
                }
                val outputUrl: String = if (treatAsVideo) {
                    val accepted = voiceApiPhase2.fastLibVideoProcess(inputFile)
                    runOnUiThread { fastLibStage = "processing" }
                    var status = voiceApiPhase2.getStatus(accepted.jobId)
                    var attempts = 0
                    while (status.status !in setOf("completed", "failed") && attempts < 120) {
                        Thread.sleep(1500)
                        status = voiceApiPhase2.getStatus(accepted.jobId)
                        attempts++
                    }
                    if (status.status != "completed") {
                        throw IllegalStateException(status.errorMessage ?: "Video cleaning failed")
                    }
                    status.outputVideoUrl ?: status.outputAudioUrl ?: status.cleanedUrl
                        ?: throw IllegalStateException("No output url for video cleaning")
                } else {
                    val accepted = voiceApiPhase2.fastLibClean(inputFile)
                    runOnUiThread { fastLibStage = "processing" }
                    var status = voiceApiPhase2.getStatus(accepted.jobId)
                    var attempts = 0
                    while (status.status !in setOf("completed", "failed") && attempts < 120) {
                        Thread.sleep(1500)
                        status = voiceApiPhase2.getStatus(accepted.jobId)
                        attempts++
                    }
                    if (status.status != "completed") {
                        throw IllegalStateException(status.errorMessage ?: "Audio cleaning failed")
                    }
                    status.outputAudioUrl ?: status.cleanedUrl
                        ?: throw IllegalStateException("No output url for audio cleaning")
                }
                runOnUiThread {
                    fastLibStage = "downloading"
                    fastLibProgress = 0
                }
                downloadFromUrl(outputUrl, outFile) { downloaded, total ->
                    val pct = if (total > 0) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else 0
                    runOnUiThread { fastLibProgress = pct }
                }

                val duration = mediaDurationSec(outFile)
                runOnUiThread {
                    fastLibIsCleaning = false
                    fastLibStage = null
                    fastLibProgress = 0
                    fastLibOutputFileName = outFile.name
                    // Newest first; keep last 20
                    fastLibOutputs.add(0, outFile.name)
                    while (fastLibOutputs.size > 20) fastLibOutputs.removeAt(fastLibOutputs.size - 1)
                    addAudioPair(timestamp, inputFile, outFile, isRecording = false)
                    Toast.makeText(this, "Fast Lib test clean done · ${"%.1f".format(duration)}s", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "startFastLibTestCleaning failed: ${e.message}", e)
                runOnUiThread {
                    fastLibIsCleaning = false
                    fastLibStage = null
                    fastLibProgress = 0
                    Toast.makeText(this, "Fast Lib test failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun uriHasVideoTrack(uri: Uri): Boolean {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(this, uri)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
        } catch (_: Exception) {
            false
        } finally {
            try {
                mmr.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun uriIsLikelyVideoMedia(uri: Uri): Boolean {
        val mime = contentResolver.getType(uri)?.lowercase(Locale.US).orEmpty()
        if (mime.startsWith("video/")) return true
        if (mime.startsWith("audio/")) return false

        val name = getFileName(uri).lowercase(Locale.US)
        val videoExt = listOf(".mp4", ".mov", ".mkv", ".webm", ".avi", ".3gp", ".m4v")
        val audioExt = listOf(".m4a", ".mp3", ".wav", ".aac", ".ogg", ".flac", ".opus")
        if (audioExt.any { name.endsWith(it) }) return false
        if (videoExt.any { name.endsWith(it) }) return true

        return uriHasVideoTrack(uri)
    }

    private fun fileHasVideoTrack(file: File): Boolean {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
        } catch (_: Exception) {
            false
        } finally {
            try {
                mmr.release()
            } catch (_: Exception) {
            }
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

        // Video files: in-app MediaPlayer needs a Surface; route to external viewer.
        if (fileName.endsWith(".mp4", ignoreCase = true) ||
            fileName.endsWith(".mov", ignoreCase = true) ||
            fileName.endsWith(".mkv", ignoreCase = true)
        ) {
            openVideoExternal(file)
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
                mime.contains("video", ignoreCase = true) && mime.contains("mp4", ignoreCase = true) -> return ".mp4"
                mime.startsWith("video/", ignoreCase = true) -> return ".mp4"
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

    private fun mediaDurationSec(file: File): Float {
        if (!file.exists()) return 0f
        if (file.name.endsWith(".wav", ignoreCase = true)) return wavDurationSec(file)
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            ms / 1000f
        } catch (_: Exception) {
            0f
        } finally {
            try {
                r.release()
            } catch (_: Exception) {
            }
        }
    }

    // Add audio pair (Original + Cleaned together)
    private fun addAudioPair(timestamp: String, originalFile: File, cleanedFile: File, isRecording: Boolean = true) {
        val time = getCurrentTimeFormatted()
        val durSec = mediaDurationSec(cleanedFile)
        consumeQuotaSeconds(durSec)
        val pair = AudioPair(
            timestamp = timestamp,
            time = time,
            originalFile = originalFile.name,
            cleanedFile = cleanedFile.name,
            isRecording = isRecording,
            durationSec = durSec
        )
        // Remove any existing pair with same timestamp
        audioPairsList.removeAll { it.timestamp == timestamp }
        // Add new pair at the beginning
        audioPairsList.add(0, pair)
        // Keep only last 10 pairs
        while (audioPairsList.size > 10) {
            audioPairsList.removeAt(audioPairsList.size - 1)
        }
        noteCleanForStreak()
    }

    private fun noteCleanForStreak() {
        val sp = getSharedPreferences("bolsaaf_streak", MODE_PRIVATE)
        val ymd = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val last = sp.getString("last_active", "") ?: ""
        if (last == ymd) return
        val yCal = Calendar.getInstance()
        yCal.add(Calendar.DAY_OF_YEAR, -1)
        val yest = SimpleDateFormat("yyyyMMdd", Locale.US).format(yCal.time)
        val prev = sp.getInt("count", 0)
        val next = if (last == yest) prev + 1 else 1
        sp.edit().putString("last_active", ymd).putInt("count", next).apply()
    }

    private fun readProfileStreak(): Int =
        getSharedPreferences("bolsaaf_streak", MODE_PRIVATE).getInt("count", 0)

    private fun quotaPrefs() = getSharedPreferences("bolsaaf_quota", MODE_PRIVATE)

    /** Load persisted free minutes. Resets to FREE_QUOTA_MINUTES at the start of each calendar month. */
    private fun loadFreeMinutes(): Int {
        val sp = quotaPrefs()
        val thisPeriod = SimpleDateFormat("yyyyMM", Locale.US).format(Date())
        val storedPeriod = sp.getString("period", null)
        return if (storedPeriod != thisPeriod) {
            sp.edit()
                .putString("period", thisPeriod)
                .putInt("left", FREE_QUOTA_MINUTES)
                .apply()
            FREE_QUOTA_MINUTES
        } else {
            sp.getInt("left", FREE_QUOTA_MINUTES).coerceIn(0, FREE_QUOTA_MINUTES)
        }
    }

    /** Decrement by seconds-processed (rounded up to minute). Warn / alert when low. */
    private fun consumeQuotaSeconds(seconds: Float) {
        if (seconds <= 0f) return
        val mins = kotlin.math.ceil(seconds / 60.0).toInt().coerceAtLeast(1)
        val prev = freeMinutesLeft
        val next = (prev - mins).coerceAtLeast(0)
        freeMinutesLeft = next
        quotaPrefs().edit().putInt("left", next).apply()
        if (prev > QUOTA_WARN_THRESHOLD && next <= QUOTA_WARN_THRESHOLD && next > 0) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "$next min free left. Upgrade to Pro for unlimited.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if (prev > 0 && next == 0) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Free quota used up. Upgrade to Pro to continue.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun totalProcessedMinutes(): Float =
        (getAudioPairs().sumOf { it.durationSec.toDouble() } / 60.0).toFloat()

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
                    lastAdaptiveProfile = null
                    adaptiveAnalysisLoading = true
                    Toast.makeText(
                        this,
                        "File ready — tap ${primaryProcessActionShortLabel()} in the dialog",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                analyzeAdaptiveHint(uri)
            } catch (e: Exception) {
                runOnUiThread {
                    isUploading = false
                    adaptiveAnalysisLoading = false
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun analyzeAdaptiveHint(uri: Uri) {
        Thread {
            try {
                val tmp = File(cacheDir, "adaptive_preview.wav")
                val capBytes = 48000 * 2 * 30 + 44
                if (!audioProcessor.exportUriAsWavCapped(uri, tmp, capBytes)) {
                    runOnUiThread {
                        lastAdaptiveProfile = null
                        adaptiveAnalysisLoading = false
                    }
                    return@Thread
                }
                val pcm = readWavPcm16Data(tmp)
                try {
                    tmp.delete()
                } catch (_: Exception) {
                }
                if (pcm == null) {
                    runOnUiThread {
                        lastAdaptiveProfile = null
                        adaptiveAnalysisLoading = false
                    }
                    return@Thread
                }
                val raw = AdaptiveAudioAnalyzer.analyze(pcm.pcm16LeToShortArray(), 48000)
                val profile = FeedbackAdaptiveMemory.applyFromFeedbackHistory(this, raw)
                runOnUiThread {
                    lastAdaptiveProfile = profile
                    adaptiveAnalysisLoading = false
                    if (cleaningPreset == CleaningPreset.NORMAL &&
                        profile.suggestedCleaningPreset != CleaningPreset.NORMAL
                    ) {
                        cleaningPreset = profile.suggestedCleaningPreset
                        Toast.makeText(
                            this,
                            "Preset → ${profile.suggestedCleaningPreset.label} (suggested)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    lastAdaptiveProfile = null
                    adaptiveAnalysisLoading = false
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
        lastAdaptiveProfile = null
        adaptiveAnalysisLoading = false
        Toast.makeText(this, "Upload cancelled", Toast.LENGTH_SHORT).show()
    }
    
    // Start cleaning process
    private fun startCleaningProcess() {
        selectedFileUri?.let { uri ->
            showCleanButton = false
            if (processingFlow == ProcessingFlow.CLEAN) {
                processAudioFileWithResult(uri)
            } else {
                processAudioFileWithPhase2Flow(uri)
            }
        }
    }

    private fun processAudioFileWithPhase2Flow(uri: Uri) {
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        if (!outputDir.exists()) outputDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val originalFile = File(outputDir, "uploaded_$timestamp${guessExtensionForUri(uri)}")
        val flowSnapshot = processingFlow
        val bgIdSnapshot = selectedBackgroundId()
        val bgVolSnapshot = bgMixVolume.coerceIn(0.02f, 0.5f)
        val outFile = if (flowSnapshot == ProcessingFlow.VIDEO_PROCESS) {
            File(outputDir, "cleaned_$timestamp.mp4")
        } else {
            File(outputDir, "cleaned_$timestamp.wav")
        }

        Thread {
            try {
                saveOriginalFromUri(uri, originalFile)
                isCleaning = true
                phase2StageName = "queued"
                phase2OverallProgress = 0
                latestReelVariantFiles = emptyMap()
                val mode = modeForPhase2Flow(cleaningPreset)
                val accepted = when (flowSnapshot) {
                    ProcessingFlow.ADD_BACKGROUND -> voiceApiPhase2.addBackground(
                        originalFile,
                        backgroundId = bgIdSnapshot,
                        bgVolume = bgVolSnapshot,
                        mode = mode
                    )
                    ProcessingFlow.REEL_MODE -> voiceApiPhase2.createReelV2(
                        file = originalFile,
                        requestedVariants = listOf("clean_only", "with_bg", "viral_boosted"),
                        targetLufs = -16f,
                        includeVideo = false,
                        backgroundPreset = bgIdSnapshot
                    )
                    ProcessingFlow.VIDEO_PROCESS -> voiceApiPhase2.processVideo(originalFile, mode = mode)
                    ProcessingFlow.CLEAN -> throw IllegalStateException("Invalid phase2 flow: CLEAN")
                }
                val outputUrl = when (flowSnapshot) {
                    ProcessingFlow.REEL_MODE -> {
                        var status = voiceApiPhase2.getReelV2Status(accepted.jobId)
                        var attempts = 0
                        val maxAttempts = 120
                        while (status.status !in setOf("completed", "failed") && attempts < maxAttempts) {
                            phase2StageName = status.currentStage ?: "processing"
                            phase2OverallProgress = status.overallProgress
                            Thread.sleep(1500)
                            status = voiceApiPhase2.getReelV2Status(accepted.jobId)
                            attempts++
                        }
                        if (status.status != "completed") {
                            throw IllegalStateException("${status.errorCode ?: "reel_failed"}: ${status.errorMessage ?: "Processing failed"}")
                        }
                        phase2StageName = status.currentStage ?: "completed"
                        phase2OverallProgress = status.overallProgress
                        val variantFiles = linkedMapOf<String, String>()
                        status.outputs.forEach { (variant, output) ->
                            val audioUrl = output.audioUrl ?: return@forEach
                            val variantFile = File(outputDir, "cleaned_${timestamp}_${variant}.wav")
                            downloadFromUrl(audioUrl, variantFile)
                            variantFiles[variant] = variantFile.name
                        }
                        if (variantFiles.isNotEmpty()) {
                            latestReelVariantFiles = variantFiles
                        }
                        val preferred = status.outputs["viral_boosted"]?.audioUrl
                            ?: status.outputs["with_bg"]?.audioUrl
                            ?: status.outputs["clean_only"]?.audioUrl
                        preferred ?: throw IllegalStateException("No Reel V2 output url in completed job")
                    }
                    else -> {
                        runOnUiThread { phase2StageName = "processing" }
                        var status = voiceApiPhase2.getStatus(accepted.jobId)
                        var attempts = 0
                        val maxAttempts = if (flowSnapshot == ProcessingFlow.VIDEO_PROCESS) 120 else 80
                        while (status.status !in setOf("completed", "failed") && attempts < maxAttempts) {
                            Thread.sleep(1500)
                            status = voiceApiPhase2.getStatus(accepted.jobId)
                            attempts++
                        }
                        if (status.status != "completed") {
                            throw IllegalStateException(status.errorMessage ?: "Processing failed")
                        }
                        when (flowSnapshot) {
                            ProcessingFlow.VIDEO_PROCESS ->
                                status.outputVideoUrl ?: status.outputAudioUrl ?: status.cleanedUrl
                            else -> status.outputAudioUrl ?: status.cleanedUrl
                        } ?: throw IllegalStateException("No output url in completed job")
                    }
                }
                runOnUiThread {
                    phase2StageName = "downloading"
                    phase2OverallProgress = 0
                }
                downloadFromUrl(outputUrl, outFile) { downloaded, total ->
                    val pct = if (total > 0) {
                        ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                    } else 0
                    runOnUiThread { phase2OverallProgress = pct }
                }
                val dSec = mediaDurationSec(outFile)
                runOnUiThread {
                    isCleaning = false
                    phase2StageName = null
                    phase2OverallProgress = 0
                    if (flowSnapshot != ProcessingFlow.REEL_MODE) {
                        latestReelVariantFiles = emptyMap()
                    }
                    selectedFileUri = null
                    lastAdaptiveProfile = null
                    adaptiveAnalysisLoading = false
                    val pair = AudioPair(
                        timestamp = timestamp,
                        time = getCurrentTimeFormatted(),
                        originalFile = originalFile.name,
                        cleanedFile = outFile.name,
                        isRecording = false,
                        durationSec = dSec
                    )
                    currentProcessedPair = pair
                    addAudioPair(timestamp, originalFile, outFile, isRecording = false)
                    showSuccess = true
                    lastSaveInfo = SaveInfo(outFile.name, dSec, timestamp)
                    Toast.makeText(this, "Done (${flowSnapshot.name}) · ${"%.1f".format(dSec)}s", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "processAudioFileWithPhase2Flow failed: ${e.message}", e)
                runOnUiThread {
                    isCleaning = false
                    phase2StageName = null
                    phase2OverallProgress = 0
                    latestReelVariantFiles = emptyMap()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun downloadFromUrl(
        url: String,
        outputFile: File,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null
    ) {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 30000
        conn.readTimeout = 120000
        val code = conn.responseCode
        if (code !in 200..299) {
            throw IllegalStateException(com.bolsaaf.audio.friendlyHttpError(code))
        }
        val total = conn.contentLengthLong.coerceAtLeast(-1L)
        conn.inputStream.use { input ->
            outputFile.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var downloaded = 0L
                var lastReport = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    downloaded += n
                    if (onProgress != null && (downloaded - lastReport >= 128 * 1024 || downloaded == total)) {
                        onProgress(downloaded, total)
                        lastReport = downloaded
                    }
                }
                onProgress?.invoke(downloaded, if (total > 0) total else downloaded)
            }
        }
    }

    private fun cloudModeForPreset(preset: CleaningPreset): String {
        return when (preset) {
            CleaningPreset.NORMAL -> "standard"
            CleaningPreset.STRONG -> "studio"
            CleaningPreset.STUDIO -> "pro"
        }
    }

    private fun cloudModeForPresetWithFallback(preset: CleaningPreset): String {
        val preferred = cloudModeForPreset(preset)
        val available = serverAvailableModes
        if (available.isEmpty()) return preferred
        return if (available.contains(preferred)) {
            preferred
        } else if (available.contains("standard")) {
            "standard"
        } else if (available.contains("basic")) {
            "basic"
        } else {
            preferred
        }
    }

    private fun modeForPhase2Flow(preset: CleaningPreset): String {
        return cloudModeForPresetWithFallback(preset)
    }

    private fun cloudModeAvailabilityNote(): String? {
        val available = serverAvailableModes
        if (available.isEmpty()) return null
        val missingStudio = !available.contains("studio")
        val missingPro = !available.contains("pro")
        return if (missingStudio || missingPro) {
            "Studio/Pro unavailable on server right now. App will use Standard mode."
        } else {
            null
        }
    }

    private fun refreshVoiceApiCapabilities() {
        Thread {
            try {
                val health = voiceApi.getHealth()
                val backgrounds = try {
                    voiceApiPhase2.getBackgrounds()
                } catch (_: Exception) {
                    emptyList()
                }
                runOnUiThread {
                    serverAvailableModes = health.availableModes
                    voiceBackgrounds = if (backgrounds.isNotEmpty()) {
                        backgrounds
                    } else {
                        listOf(VoiceBackground("rain", "Rain", null, 0.15f))
                    }
                    selectedBackgroundIndex =
                        selectedBackgroundIndex.coerceIn(0, (voiceBackgrounds.size - 1).coerceAtLeast(0))
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (voiceBackgrounds.isEmpty()) {
                        voiceBackgrounds = listOf(VoiceBackground("rain", "Rain", null, 0.15f))
                    }
                }
            }
        }.start()
    }

    private fun primaryProcessActionShortLabel(): String =
        when (processingFlow) {
            ProcessingFlow.REEL_MODE -> "Make Reel"
            ProcessingFlow.CLEAN -> "Quick clean"
            ProcessingFlow.VIDEO_PROCESS -> "Process video"
            else -> "Start"
        }

    private fun selectedBackgroundId(): String =
        voiceBackgrounds.getOrNull(selectedBackgroundIndex.coerceIn(0, voiceBackgrounds.lastIndex.coerceAtLeast(0)))
            ?.id?.takeIf { it.isNotBlank() } ?: "rain"

    private fun processPrimaryButtonLabel(): String =
        when (processingFlow) {
            ProcessingFlow.CLEAN -> "Clean Audio"
            ProcessingFlow.VIDEO_PROCESS -> "Process Video"
            ProcessingFlow.ADD_BACKGROUND -> "Apply vibe"
            ProcessingFlow.REEL_MODE -> "Make Reel"
        }

    private fun processHelperText(): String =
        when (processingFlow) {
            ProcessingFlow.CLEAN -> "Noise reduce + enhance (cloud or local)."
            ProcessingFlow.VIDEO_PROCESS -> "Clean the audio track and remux to video (server job)."
            ProcessingFlow.ADD_BACKGROUND -> "Mix voice with the vibe you pick below."
            ProcessingFlow.REEL_MODE -> "Reel pipeline: clean → vibe (server) → loudness. Video export when server is ready."
        }

    private fun processingDialogTitle(): String =
        when (processingFlow) {
            ProcessingFlow.VIDEO_PROCESS -> {
                val stage = phase2StageName
                val pct = phase2OverallProgress.coerceIn(0, 100)
                when (stage) {
                    "downloading" -> if (pct > 0) "Downloading Video… $pct%" else "Downloading Video…"
                    "processing" -> "Cleaning Audio Track…"
                    else -> "Processing Video…"
                }
            }
            ProcessingFlow.REEL_MODE -> {
                val pct = phase2OverallProgress.coerceIn(0, 100)
                if (pct > 0) "Making Reel… $pct%" else "Making Reel…"
            }
            else -> "Processing…"
        }

    private fun processingDialogSubtitle(): String =
        when (processingFlow) {
            ProcessingFlow.VIDEO_PROCESS -> {
                when (phase2StageName) {
                    "downloading" -> "Saving cleaned video to your phone…"
                    "processing" -> "Server is cleaning the audio and rebuilding the file."
                    else -> "Uploading and queuing your video on the server…"
                }
            }
            ProcessingFlow.REEL_MODE -> {
                val stage = phase2StageName?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
                if (stage.isNullOrBlank()) {
                    "Analyzing → Cleaning → Mixing → Encoding"
                } else {
                    "Stage: $stage"
                }
            }
            else -> "Server job running — please wait…"
        }

    private data class CloudBatchPlan(
        val inputWav: File,
        val chunkFiles: List<File>
    )

    private fun intToLeBytes(v: Int): ByteArray {
        return byteArrayOf(
            (v and 0xff).toByte(),
            ((v ushr 8) and 0xff).toByte(),
            ((v ushr 16) and 0xff).toByte(),
            ((v ushr 24) and 0xff).toByte()
        )
    }

    private fun shortToLeBytes(v: Int): ByteArray {
        return byteArrayOf((v and 0xff).toByte(), ((v ushr 8) and 0xff).toByte())
    }

    private fun writeMono48kWavFromPcm16(pcm: ByteArray, outFile: File) {
        val sampleRate = 48000
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)
        val totalDataLen = pcm.size + 36
        outFile.outputStream().use { out ->
            out.write("RIFF".toByteArray())
            out.write(intToLeBytes(totalDataLen))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToLeBytes(16))
            out.write(shortToLeBytes(1)) // PCM
            out.write(shortToLeBytes(channels))
            out.write(intToLeBytes(sampleRate))
            out.write(intToLeBytes(byteRate))
            out.write(shortToLeBytes(blockAlign))
            out.write(shortToLeBytes(bitsPerSample))
            out.write("data".toByteArray())
            out.write(intToLeBytes(pcm.size))
            out.write(pcm)
        }
    }

    private fun readWavPcm16Data(file: File): ByteArray? {
        if (!file.exists() || file.length() < 44) return null
        val b = file.readBytes()
        if (!(b[0] == 'R'.code.toByte() && b[1] == 'I'.code.toByte() && b[2] == 'F'.code.toByte() && b[3] == 'F'.code.toByte())) {
            return null
        }
        var off = 12
        while (off + 8 <= b.size) {
            val id = String(b, off, 4, Charsets.US_ASCII)
            val sz = (b[off + 4].toInt() and 0xff) or
                ((b[off + 5].toInt() and 0xff) shl 8) or
                ((b[off + 6].toInt() and 0xff) shl 16) or
                ((b[off + 7].toInt() and 0xff) shl 24)
            val dataStart = off + 8
            if (id == "data") {
                val end = (dataStart + sz).coerceAtMost(b.size)
                return b.copyOfRange(dataStart, end)
            }
            off = dataStart + sz + (sz and 1)
        }
        return null
    }


    private fun pcmRmsDbfs(pcm: ByteArray): Float {
        if (pcm.size < 2) return -120f
        val s = pcm.pcm16LeToShortArray()
        if (s.isEmpty()) return -120f
        var sum = 0.0
        for (v in s) {
            val x = v.toDouble()
            sum += x * x
        }
        val rms = sqrt(sum / s.size).toFloat()
        if (rms <= 1e-9f) return -120f
        return (20f * log10(rms / 32768f)).coerceIn(-120f, 0f)
    }

    private fun applyMinLoudnessFloor(wavFile: File, minRmsDbfs: Float = -42f, maxBoostDb: Float = 18f): Boolean {
        val pcm = readWavPcm16Data(wavFile) ?: return false
        val currentDb = pcmRmsDbfs(pcm)
        if (currentDb >= minRmsDbfs) return false
        val neededDb = (minRmsDbfs - currentDb).coerceAtMost(maxBoostDb)
        val gain = 10f.pow(neededDb / 20f)
        val inShort = pcm.pcm16LeToShortArray()
        if (inShort.isEmpty()) return false
        val out = ByteArray(inShort.size * 2)
        var p = 0
        for (v in inShort) {
            val y = (v * gain).toInt().coerceIn(-32768, 32767)
            out[p++] = (y and 0xff).toByte()
            out[p++] = ((y ushr 8) and 0xff).toByte()
        }
        writeMono48kWavFromPcm16(out, wavFile)
        Log.i(TAG, "Applied loudness floor min=${minRmsDbfs}dBFS gain=${"%.1f".format(neededDb)}dB")
        return true
    }

    private fun applyDryMixFromOriginal(originalWav: File, cleanedWav: File, dryMix: Float): Boolean {
        if (dryMix <= 0f) return false
        val oPcm = readWavPcm16Data(originalWav) ?: return false
        val cPcm = readWavPcm16Data(cleanedWav) ?: return false
        val o = oPcm.pcm16LeToShortArray()
        val c = cPcm.pcm16LeToShortArray()
        val n = minOf(o.size, c.size)
        if (n <= 0) return false
        val dry = dryMix.coerceIn(0f, 0.4f)
        val wet = 1f - dry
        val out = ByteArray(n * 2)
        var p = 0
        for (i in 0 until n) {
            val y = (c[i] * wet + o[i] * dry).toInt().coerceIn(-32768, 32767)
            out[p++] = (y and 0xff).toByte()
            out[p++] = ((y ushr 8) and 0xff).toByte()
        }
        writeMono48kWavFromPcm16(out, cleanedWav)
        Log.i(TAG, "Applied dry mix=${"%.2f".format(dry)}")
        return true
    }

    private fun prepareCloudBatchInputs(uri: Uri, outputDir: File, timestamp: String): CloudBatchPlan? {
        val maxCloudBytes = 5 * 1024 * 1024
        val maxPcmBytesPerChunk = (maxCloudBytes - 44).coerceAtLeast(1024)
        val sourceWav = File(outputDir, "uploaded_${timestamp}_cloud_source.wav")
        val exported = audioProcessor.exportUriAsWav(uri, sourceWav)
        if (!exported || !sourceWav.exists()) return null
        val pcm = readWavPcm16Data(sourceWav) ?: return null

        if (sourceWav.length() <= maxCloudBytes.toLong()) {
            return CloudBatchPlan(sourceWav, listOf(sourceWav))
        }

        val chunkFiles = ArrayList<File>()
        var start = 0
        var idx = 1
        while (start < pcm.size) {
            val endRaw = (start + maxPcmBytesPerChunk).coerceAtMost(pcm.size)
            val end = if ((endRaw - start) % 2 == 0) endRaw else endRaw - 1
            if (end <= start) break
            val partPcm = pcm.copyOfRange(start, end)
            val partFile = File(outputDir, "uploaded_${timestamp}_cloud_part%02d.wav".format(idx))
            writeMono48kWavFromPcm16(partPcm, partFile)
            chunkFiles.add(partFile)
            start = end
            idx++
        }
        if (chunkFiles.isEmpty()) return null
        return CloudBatchPlan(sourceWav, chunkFiles)
    }

    private fun mergeCleanedWavChunks(cleanedChunks: List<File>, outputWav: File): Boolean {
        return try {
            if (cleanedChunks.isEmpty()) return false
            if (cleanedChunks.size == 1) {
                cleanedChunks.first().copyTo(outputWav, overwrite = true)
                return true
            }
            val mergedPcm = ArrayList<ByteArray>()
            var total = 0
            for (f in cleanedChunks) {
                val pcm = readWavPcm16Data(f) ?: return false
                mergedPcm.add(pcm)
                total += pcm.size
            }
            val all = ByteArray(total)
            var p = 0
            for (c in mergedPcm) {
                c.copyInto(all, p)
                p += c.size
            }
            writeMono48kWavFromPcm16(all, outputWav)
            true
        } catch (_: Exception) {
            false
        }
    }
    
    // Process audio file with result tracking
    private fun processAudioFileWithResult(uri: Uri) {
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "BolSaaf")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val originalFile = File(outputDir, "uploaded_$timestamp${guessExtensionForUri(uri)}")
        val cleanedFile = File(outputDir, "cleaned_$timestamp.wav")
        
        Thread {
            try {
                val modesSnap = serverAvailableModes.toSet()
                saveOriginalFromUri(uri, originalFile)
                isCleaning = true

                val batchPlan = prepareCloudBatchInputs(uri, outputDir, timestamp)
                var success = false
                var usedCloud = false
                var cloudProcessingTimeSec: Float? = null
                var cloudChunkCount = 0

                var runtimeAdaptiveProfile: AdaptiveAudioAnalyzer.Profile? = null
                batchPlan?.let { plan ->
                    readWavPcm16Data(plan.inputWav)?.let { pcmBytes ->
                        try {
                            val raw = AdaptiveAudioAnalyzer.analyze(pcmBytes.pcm16LeToShortArray(), 48000)
                            val profile = FeedbackAdaptiveMemory.applyFromFeedbackHistory(this, raw)
                            runtimeAdaptiveProfile = profile
                            Log.i(
                                TAG,
                                "Adaptive preset applied: mode=${profile.adaptivePreset.mode} " +
                                    "denoise=${profile.adaptivePreset.denoiseLevel} " +
                                    "compressor=${profile.adaptivePreset.compressorStrength} " +
                                    "preGain=${"%.1f".format(profile.adaptivePreset.preGain)} " +
                                    "dryMix=${"%.2f".format(profile.adaptivePreset.dryMix)} " +
                                    "conf=${"%.2f".format(profile.confidence)}"
                            )
                        } catch (_: Exception) {
                        }
                    }
                }

                if (batchPlan != null) {
                    try {
                        val cleanedParts = ArrayList<File>()
                        var totalCloudSec = 0f
                        var hasCloudSec = false
                        val selectedModeBase = runtimeAdaptiveProfile?.adaptivePreset?.mode?.let { m ->
                            if (modesSnap.isEmpty() || modesSnap.contains(m)) m else cloudModeForPresetWithFallback(cleaningPreset)
                        } ?: cloudModeForPresetWithFallback(cleaningPreset)
                        val selectedMode = selectedModeBase
                        val requestedMode = cloudModeForPreset(cleaningPreset)
                        Log.d(
                            TAG,
                            "Cloud clean start requestedMode=$requestedMode selectedMode=$selectedMode chunks=${batchPlan.chunkFiles.size}"
                        )
                        if (selectedMode != requestedMode) {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Server mode fallback: $requestedMode -> $selectedMode",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        batchPlan.chunkFiles.forEachIndexed { i, chunk ->
                            val outPart = File(outputDir, "cleaned_${timestamp}_part%02d.wav".format(i + 1))
                            val cloud = voiceApi.cleanAudio(
                                audioFile = chunk,
                                outputFile = outPart,
                                mode = selectedMode
                            )
                            if (cloud.processingTimeSec != null) {
                                totalCloudSec += cloud.processingTimeSec
                                hasCloudSec = true
                            }
                            cleanedParts.add(cloud.outputFile)
                            Log.d(
                                TAG,
                                "Cloud chunk complete index=${i + 1}/${batchPlan.chunkFiles.size} output=${cloud.outputFile.name} sec=${cloud.processingTimeSec}"
                            )
                        }
                        success = mergeCleanedWavChunks(cleanedParts, cleanedFile)
                        Log.d(TAG, "Cloud merge success=$success output=${cleanedFile.name}")
                        usedCloud = success
                        cloudChunkCount = batchPlan.chunkFiles.size
                        cloudProcessingTimeSec = if (hasCloudSec) totalCloudSec else null
                        if (success && batchPlan.chunkFiles.size == 1) {
                            val oPcm = readWavPcm16Data(batchPlan.inputWav)
                            val cPcm = readWavPcm16Data(cleanedFile)
                            if (oPcm != null && cPcm != null) {
                                val oShort = oPcm.pcm16LeToShortArray()
                                var finalQ = ProcessingQualityGuard.compare(oShort, cPcm.pcm16LeToShortArray())
                                if (!finalQ.pass) {
                                    Log.w(
                                        TAG,
                                        "Quality guard issues=${finalQ.issues} dRMS=${"%.2f".format(finalQ.rmsDeltaDb)} " +
                                            "dPeak=${"%.2f".format(finalQ.peakDeltaDb)}"
                                    )
                                    val mild = milderCloudMode(selectedMode, modesSnap)
                                    if (mild != null) {
                                        try {
                                            val chunk = batchPlan.chunkFiles.first()
                                            val outRetry = File(outputDir, "cleaned_${timestamp}_retry.wav")
                                            voiceApi.cleanAudio(
                                                audioFile = chunk,
                                                outputFile = outRetry,
                                                mode = mild
                                            )
                                            val retryPcm = readWavPcm16Data(outRetry)
                                            if (retryPcm != null) {
                                                val q2 = ProcessingQualityGuard.compare(
                                                    oShort,
                                                    retryPcm.pcm16LeToShortArray()
                                                )
                                                val better = q2.pass || q2.rmsDeltaDb > finalQ.rmsDeltaDb
                                                if (better) {
                                                    outRetry.copyTo(cleanedFile, overwrite = true)
                                                    finalQ = q2
                                                    Log.i(
                                                        TAG,
                                                        "Quality retry applied mode=$mild pass=${q2.pass}"
                                                    )
                                                    runOnUiThread {
                                                        Toast.makeText(
                                                            this,
                                                            "Gentler clean applied ($mild)",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                            try {
                                                outRetry.delete()
                                            } catch (_: Exception) {
                                            }
                                        } catch (e: Exception) {
                                            Log.d(TAG, "Quality retry skipped: ${e.message}")
                                        }
                                    }

                                    val dryMix = runtimeAdaptiveProfile?.adaptivePreset?.dryMix?.toFloat() ?: 0f
                                    if (dryMix > 0f) {
                                        try {
                                            applyDryMixFromOriginal(batchPlan.inputWav, cleanedFile, dryMix)
                                        } catch (e: Exception) {
                                            Log.d(TAG, "Dry mix skipped: ${e.message}")
                                        }
                                    }

                                    if (finalQ.issues.contains("output_very_quiet")) {
                                        try {
                                            val targetDb = if ((runtimeAdaptiveProfile?.adaptivePreset?.preGain ?: 2.5) >= 4.0) -36f else -40f
                                            val boosted = applyMinLoudnessFloor(cleanedFile, minRmsDbfs = targetDb)
                                            if (boosted) {
                                                runOnUiThread {
                                                    Toast.makeText(
                                                        this,
                                                        "Output boosted for audibility",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.d(TAG, "Loudness floor skipped: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        success = false
                    }
                }

                if (!success) {
                    Log.d(TAG, "Falling back to local cleaning path")
                    if (!audioProcessor.initialize()) {
                        throw IllegalStateException("Failed to initialize local cleaner")
                    }
                    audioProcessor.cleaningPreset =
                        runtimeAdaptiveProfile?.adaptivePreset?.toCleaningPreset() ?: cleaningPreset
                    success = audioProcessor.cleanAudioFile(uri, cleanedFile) { _ -> }
                    audioProcessor.destroy()
                }

                runOnUiThread {
                    isCleaning = false
                    selectedFileUri = null
                    lastAdaptiveProfile = null
                    adaptiveAnalysisLoading = false
                    
                    if (success) {
                        val dSec = wavDurationSec(cleanedFile)
                        Log.d(TAG, "Cleaning success usedCloud=$usedCloud durationSec=$dSec file=${cleanedFile.name}")
                        val pair = AudioPair(
                            timestamp = timestamp,
                            time = getCurrentTimeFormatted(),
                            originalFile = originalFile.name,
                            cleanedFile = cleanedFile.name,
                            isRecording = false,
                            durationSec = dSec
                        )
                        currentProcessedPair = pair
                        addAudioPair(timestamp, originalFile, cleanedFile, isRecording = false)
                        showSuccess = true
                        lastSaveInfo = SaveInfo(cleanedFile.name, dSec, timestamp)
                        val engine = if (usedCloud) {
                            val base = if (cloudProcessingTimeSec != null) "Cloud ${"%.1f".format(cloudProcessingTimeSec)}s" else "Cloud"
                            if (cloudChunkCount > 1) "$base · ${cloudChunkCount} parts" else base
                        } else "Local"
                        Toast.makeText(this, "Cleaned ($engine) · ${"%.1f".format(dSec)}s", Toast.LENGTH_LONG).show()
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
                Log.e(TAG, "processAudioFileWithResult failed: ${e.message}", e)
                runOnUiThread {
                    isCleaning = false
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun submitUserFeedback(
        pair: AudioPair,
        clearVoice: Boolean,
        issueType: String?,
        issueTimestamp: String?,
        notes: String?
    ) {
        Thread {
            FeedbackAdaptiveMemory.record(this@MainActivity, clearVoice, issueType, notes)
            val mode = when (processingFlow) {
                ProcessingFlow.REEL_MODE -> "reel"
                ProcessingFlow.CLEAN -> "clean"
                ProcessingFlow.ADD_BACKGROUND -> "add_background"
                ProcessingFlow.VIDEO_PROCESS -> "video_process"
            }
            val issueNorm = issueType?.trim().orEmpty().lowercase(Locale.US)
            val resultLabel = if (clearVoice) {
                "good"
            } else {
                when {
                    issueNorm.contains("quiet") -> "quiet"
                    issueNorm.contains("artifact") -> "artifacts"
                    else -> "artifacts"
                }
            }
            val payload = JSONObject().apply {
                put("app_version", "android-debug")
                put("device_model", Build.MODEL)
                put("os_version", "Android ${Build.VERSION.RELEASE}")
                put("sample_timestamp", pair.timestamp)
                put("mode_used", mode)
                put("input_type", "unknown")
                put("result_label", resultLabel)
                put("issue_timestamp", issueTimestamp?.trim().orEmpty())
                put("notes", notes?.trim().orEmpty())
                put("cleaned_file", pair.cleanedFile)
                put("extra_meta", JSONObject().apply {
                    put("clear_voice", clearVoice)
                    put("issue_type", issueType?.trim().orEmpty())
                    put("is_recording", pair.isRecording)
                    put("duration_sec", pair.durationSec)
                })
            }
            val ok = postFeedbackToApi(payload) || appendFeedbackLocally(payload)
            runOnUiThread {
                if (ok) {
                    Toast.makeText(this, "Feedback sent. Thanks!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Feedback save failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun postFeedbackToApi(payload: JSONObject): Boolean {
        return try {
            val endpoint = "https://shadowselfwork.com/voice/feedback/"
            val conn = java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun appendFeedbackLocally(payload: JSONObject): Boolean {
        return try {
            val dir = File(getExternalFilesDir(null), "feedback")
            if (!dir.exists()) dir.mkdirs()
            val out = File(dir, "feedback_queue.jsonl")
            out.appendText(payload.toString() + "\n")
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openExternalUrl(url: String) {
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSupportEmail() {
        val subject = "BolSaaf support — v" + try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.1"
        } catch (_: Exception) { "1.0.1" }
        val uri = android.net.Uri.parse(
            "mailto:${SUPPORT_EMAIL}?subject=${android.net.Uri.encode(subject)}"
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, uri).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(android.content.Intent.createChooser(intent, "Contact BolSaaf support"))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No email app found. Reach us at $SUPPORT_EMAIL",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openVideoExternal(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(android.content.Intent.createChooser(intent, "Open video with"))
        } catch (e: Exception) {
            Toast.makeText(this, "No video player found", Toast.LENGTH_SHORT).show()
        }
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
        
        val mime = when {
            fileName.endsWith(".mp4", true) -> "video/mp4"
            fileName.endsWith(".wav", true) -> "audio/wav"
            fileName.endsWith(".mp3", true) -> "audio/mpeg"
            fileName.endsWith(".m4a", true) -> "audio/mp4"
            else -> "*/*"
        }
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = mime
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "BolSaaf output")
            putExtra(android.content.Intent.EXTRA_TEXT, "Shared from BolSaaf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Share"))
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
