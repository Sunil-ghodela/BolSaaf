package com.reelvoice.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelvoice.ui.VibeUi
import com.reelvoice.ui.theme.AccentCyan
import com.reelvoice.ui.theme.AccentGreen
import com.reelvoice.ui.theme.AccentPurple
import com.reelvoice.ui.theme.BackgroundCard
import com.reelvoice.ui.theme.BackgroundDark
import com.reelvoice.ui.theme.BrandGradient
import com.reelvoice.ui.theme.CtaOrangeRedGradient
import com.reelvoice.ui.theme.NavUnselected
import com.reelvoice.ui.theme.PanelOverlay
import com.reelvoice.ui.theme.PrimaryGradient
import com.reelvoice.ui.theme.SliderTrack
import com.reelvoice.ui.theme.SliderTrackStrong
import com.reelvoice.ui.theme.SubtitleBluePurple
import com.reelvoice.ui.theme.SurfaceStripe
import com.reelvoice.ui.theme.TextPrimary
import com.reelvoice.ui.theme.TextSecondary
import com.reelvoice.ui.theme.ThemeBlue
import com.reelvoice.ui.theme.ThemeBlueLight
import com.reelvoice.ui.theme.ThemeRed
import com.reelvoice.ui.theme.ThemeRedLight
import com.reelvoice.ui.theme.TitleVideoAccent
import com.reelvoice.audio.AdaptiveAudioAnalyzer
import com.reelvoice.audio.CleaningPreset
import com.reelvoice.audio.FillerRemover
import com.reelvoice.audio.SilenceCutter
import com.reelvoice.audio.VoiceStyle
import com.reelvoice.audio.VoiceStyleProcessor
import com.reelvoice.audio.WavIo
import com.reelvoice.audio.WavPreview
import com.reelvoice.ui.animation.MD3Motion
import com.reelvoice.ui.animation.slideInFromStart
import com.reelvoice.ui.components.BeforeAfterFormatPicker
import com.reelvoice.ui.components.BottomNavBar
import com.reelvoice.ui.components.ModePresetStrip
import com.reelvoice.ui.components.ReelTemplateSheet
import com.reelvoice.ui.components.VoiceStyleSheet
import com.reelvoice.ui.animation.slideInFromBottom
import com.reelvoice.ui.animation.slideOutToBottom
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.reelvoice.video.BeforeAfterImageGenerator
import com.reelvoice.video.BeforeAfterVideoExport
import com.reelvoice.video.FrameRenderer
import com.reelvoice.video.PhotoLoader
import com.reelvoice.video.PhotoWaveformFrameRenderer
import com.reelvoice.video.ReelTemplate
import com.reelvoice.video.WaveformFrameRenderer
import com.reelvoice.video.WaveformVideoEncoder
import com.reelvoice.video.WaveformWindowSampler

data class SaveInfo(
    val cleanedFileName: String,
    val durationSec: Float,
    val timestamp: String
)

@Composable
fun HomeScreen(
    recordingsDir: File,
    isRecording: Boolean = false,
    isCleaning: Boolean = false,
    isUploading: Boolean = false,
    uploadProgress: Int = 0,
    showSuccess: Boolean = false,
    lastSaveInfo: SaveInfo? = null,
    showCleanButton: Boolean = false,
    selectedFileName: String? = null,
    selectedTab: Int = 0,
    cleaningPreset: CleaningPreset = CleaningPreset.NORMAL,
    modeAvailabilityNote: String? = null,
    processingModes: List<String> = listOf(
        "Make Reel ★",
        "Quick clean",
        "BG mix",
        "Video"
    ),
    selectedProcessingModeIndex: Int = 0,
    onProcessingModeChange: (Int) -> Unit = {},
    showBackgroundControls: Boolean = false,
    backgroundLabels: List<String> = emptyList(),
    selectedBackgroundIndex: Int = 0,
    onBackgroundIndexChange: (Int) -> Unit = {},
    bgMixVolume: Float = 0.15f,
    onBgMixVolumeChange: (Float) -> Unit = {},
    processPrimaryButtonLabel: String = "Clean Audio",
    processHelperText: String = "Tap to remove noise and enhance your audio file.",
    processingDialogTitle: String = "Cleaning Audio...",
    processingDialogSubtitle: String = "AI is removing noise and enhancing your audio. Please wait...",
    onCleaningPresetChange: (CleaningPreset) -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onUploadFile: () -> Unit = {},
    onVideoUpload: () -> Unit = {},
    onCleanFile: () -> Unit = {},
    onCancelUpload: () -> Unit = {},
    onGoToHistory: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onDismissSuccess: () -> Unit = {},
    audioPairs: List<AudioPair> = emptyList(),
    currentProcessedPair: AudioPair? = null,
    freeMinutesLeft: Int = 20,
    freeQuotaMinutes: Int = 20,
    currentlyPlaying: String? = null,
    onPlayFile: (String) -> Unit = {},
    onStopFile: () -> Unit = {},
    onRemovePair: (String) -> Unit = {},
    onShareFile: (String) -> Unit = {},
    onDownloadFile: (String) -> Unit = {},
    onSubmitFeedback: (AudioPair, Boolean, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    adaptiveProfile: AdaptiveAudioAnalyzer.Profile? = null,
    adaptiveAnalysisLoading: Boolean = false,
    onApplyAdaptivePreset: () -> Unit = {},
    onProminentReelClick: () -> Unit = {},
    onPickModePreset: (com.reelvoice.audio.ModePreset) -> Unit = {},
    reelVariantFiles: Map<String, String> = emptyMap(),
    onPlayReelVariant: (String) -> Unit = {},
    onShareReelVariant: (String) -> Unit = {},
    onDownloadReelVariant: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var feedbackPair by remember { mutableStateOf<AudioPair?>(null) }
    var showVibeSheet by remember { mutableStateOf(false) }
    var videoExportProgress by remember { mutableStateOf<Float?>(null) }
    var videoExportError by remember { mutableStateOf<String?>(null) }
    var beforeAfterInFlight by remember { mutableStateOf(false) }
    var beforeAfterError by remember { mutableStateOf<String?>(null) }
    var beforeAfterProgress by remember { mutableStateOf<Float?>(null) }
    var beforeAfterPickerPair by remember { mutableStateOf<AudioPair?>(null) }
    var tightenInFlight by remember { mutableStateOf(false) }
    var tightenMessage by remember { mutableStateOf<String?>(null) }
    var waveformStylePickerPair by remember { mutableStateOf<AudioPair?>(null) }
    var pendingPhotoWaveformPair by remember { mutableStateOf<AudioPair?>(null) }
    var voiceStylePickerPair by remember { mutableStateOf<AudioPair?>(null) }
    var voiceStyleInFlight by remember { mutableStateOf(false) }
    var voiceStyleMessage by remember { mutableStateOf<String?>(null) }
    var fillerInFlight by remember { mutableStateOf(false) }
    var fillerMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val exportScope = rememberCoroutineScope()

    fun startWaveformExport(
        pair: AudioPair,
        photoUri: android.net.Uri? = null,
        template: ReelTemplate? = null,
    ) {
        if (videoExportProgress != null) return
        val sourceFile = File(recordingsDir, pair.cleanedFile)
        if (!sourceFile.exists() || sourceFile.length() < 44) {
            videoExportError = "Source audio file not found"
            return
        }
        videoExportProgress = 0f
        videoExportError = null
        exportScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val decoded = WaveformWindowSampler.decodeWav(sourceFile)
                        ?: error("Only 16-bit PCM WAV is supported")
                    val sampler = WaveformWindowSampler(decoded.samples, decoded.sampleRate)
                    val renderer: FrameRenderer = if (photoUri != null) {
                        val bitmap = PhotoLoader.loadScaled(
                            contentResolver = context.contentResolver,
                            uri = photoUri,
                            targetWidth = WaveformVideoEncoder.DEFAULT_WIDTH,
                            targetHeight = WaveformVideoEncoder.DEFAULT_HEIGHT
                        )
                        PhotoWaveformFrameRenderer(
                            width = WaveformVideoEncoder.DEFAULT_WIDTH,
                            height = WaveformVideoEncoder.DEFAULT_HEIGHT,
                            background = bitmap,
                            title = "ReelVoice",
                            subtitle = "cleaned with AI"
                        )
                    } else {
                        WaveformFrameRenderer(
                            width = WaveformVideoEncoder.DEFAULT_WIDTH,
                            height = WaveformVideoEncoder.DEFAULT_HEIGHT,
                            title = template?.titleText ?: "ReelVoice",
                            subtitle = template?.subtitleText ?: "cleaned with AI",
                            template = template
                        )
                    }
                    val encoder = WaveformVideoEncoder(sampler, renderer)
                    val suffix = when {
                        photoUri != null -> "_photoreel"
                        template != null -> "_${template.id}"
                        else -> "_waveform"
                    }
                    val outName = sourceFile.nameWithoutExtension + suffix + ".mp4"
                    val outFile = File(recordingsDir, outName)
                    encoder.encode(outFile) { frac ->
                        videoExportProgress = frac
                    }
                    outName
                }
            }
            videoExportProgress = null
            result
                .onSuccess { fileName -> onShareFile(fileName) }
                .onFailure { err -> videoExportError = err.message ?: "Video export failed" }
        }
    }

    fun startBeforeAfterImageExport(
        pair: AudioPair,
        aspect: BeforeAfterImageGenerator.Aspect
    ) {
        if (beforeAfterInFlight) return
        val originalFile = File(recordingsDir, pair.originalFile)
        val cleanedFile = File(recordingsDir, pair.cleanedFile)
        if (!originalFile.exists() || !cleanedFile.exists()) {
            beforeAfterError = "Source files not found"
            return
        }
        beforeAfterInFlight = true
        beforeAfterError = null
        beforeAfterProgress = null
        exportScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val suffix = when (aspect) {
                        BeforeAfterImageGenerator.Aspect.PORTRAIT_9_16 -> "9x16"
                        BeforeAfterImageGenerator.Aspect.SQUARE_1_1 -> "1x1"
                    }
                    val outName = "before_after_${pair.timestamp}_$suffix.png"
                    val outFile = File(recordingsDir, outName)
                    BeforeAfterImageGenerator.generate(
                        originalWav = originalFile,
                        cleanedWav = cleanedFile,
                        output = outFile,
                        aspect = aspect
                    )
                    outName
                }
            }
            beforeAfterInFlight = false
            result
                .onSuccess { fileName -> onShareFile(fileName) }
                .onFailure { err -> beforeAfterError = err.message ?: "Share card generation failed" }
        }
    }

    fun startApplyVoiceStyle(pair: AudioPair, style: VoiceStyle) {
        if (voiceStyleInFlight) return
        if (style == VoiceStyle.NONE) {
            voiceStyleMessage = "No style applied. Re-clean the clip to start fresh."
            return
        }
        val cleanedFile = File(recordingsDir, pair.cleanedFile)
        if (!cleanedFile.exists()) {
            voiceStyleMessage = "Cleaned file not found"
            return
        }
        voiceStyleInFlight = true
        exportScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val decoded = WaveformWindowSampler.decodeWav(cleanedFile)
                        ?: error("Only 16-bit PCM WAV is supported")
                    val styled = VoiceStyleProcessor.process(decoded.samples, decoded.sampleRate, style)
                    WavIo.writeWav(styled, cleanedFile, decoded.sampleRate)
                    "${style.emoji} ${style.label} style applied"
                }
            }
            voiceStyleInFlight = false
            result
                .onSuccess { msg -> voiceStyleMessage = msg }
                .onFailure { err -> voiceStyleMessage = err.message ?: "Voice style failed" }
        }
    }

    fun startTrimFillers(pair: AudioPair) {
        if (fillerInFlight) return
        val cleanedFile = File(recordingsDir, pair.cleanedFile)
        if (!cleanedFile.exists()) {
            fillerMessage = "Cleaned file not found"
            return
        }
        fillerInFlight = true
        exportScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val decoded = WaveformWindowSampler.decodeWav(cleanedFile)
                        ?: error("Only 16-bit PCM WAV is supported")
                    val report = FillerRemover.process(decoded.samples, decoded.sampleRate)
                    if (report.fillersRemoved == 0) {
                        "No clear fillers detected (beta)"
                    } else {
                        WavIo.writeWav(report.output, cleanedFile, decoded.sampleRate)
                        "Trimmed ${report.fillersRemoved} filler${if (report.fillersRemoved == 1) "" else "s"} · saved ${"%.1f".format(report.secondsRemoved)}s"
                    }
                }
            }
            fillerInFlight = false
            result
                .onSuccess { msg -> fillerMessage = msg }
                .onFailure { err -> fillerMessage = err.message ?: "Filler trim failed" }
        }
    }

    fun startTightenSilences(pair: AudioPair) {
        if (tightenInFlight) return
        val cleanedFile = File(recordingsDir, pair.cleanedFile)
        if (!cleanedFile.exists()) {
            tightenMessage = "Cleaned file not found"
            return
        }
        tightenInFlight = true
        exportScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val decoded = WaveformWindowSampler.decodeWav(cleanedFile)
                        ?: error("Only 16-bit PCM WAV is supported")
                    val report = SilenceCutter.process(decoded.samples, decoded.sampleRate)
                    if (report.samplesRemoved == 0) {
                        "No long pauses found — nothing to trim"
                    } else {
                        WavIo.writeWav(report.output, cleanedFile, decoded.sampleRate)
                        "Trimmed ${"%.1f".format(report.secondsRemoved)}s of pauses"
                    }
                }
            }
            tightenInFlight = false
            result
                .onSuccess { msg -> tightenMessage = msg }
                .onFailure { err -> tightenMessage = err.message ?: "Tighten failed" }
        }
    }

    fun startBeforeAfterVideoExport(pair: AudioPair) {
        if (beforeAfterInFlight) return
        val originalFile = File(recordingsDir, pair.originalFile)
        val cleanedFile = File(recordingsDir, pair.cleanedFile)
        if (!originalFile.exists() || !cleanedFile.exists()) {
            beforeAfterError = "Source files not found"
            return
        }
        beforeAfterInFlight = true
        beforeAfterError = null
        beforeAfterProgress = 0f
        exportScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val outName = "before_after_${pair.timestamp}.mp4"
                    val outFile = File(recordingsDir, outName)
                    BeforeAfterVideoExport.generate(
                        originalWav = originalFile,
                        cleanedWav = cleanedFile,
                        output = outFile
                    ) { frac -> beforeAfterProgress = frac }
                    outName
                }
            }
            beforeAfterInFlight = false
            beforeAfterProgress = null
            result
                .onSuccess { fileName -> onShareFile(fileName) }
                .onFailure { err -> beforeAfterError = err.message ?: "Video export failed" }
        }
    }

    // Declared after startWaveformExport so its lambda can call it without a
    // forward reference — Kotlin requires local functions to be in scope at
    // lambda-capture time.
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        val pair = pendingPhotoWaveformPair
        pendingPhotoWaveformPair = null
        if (pair != null && uri != null) {
            startWaveformExport(pair, photoUri = uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        
        // Primary accent glow at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0f),
                        radius = 0.5f
                    )
                )
        )
        
        // Secondary accent glow at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 1f),
                        radius = 0.5f
                    )
                )
        )
        // Avoid fullscreen animated overlay while recording (it was causing visual breakage).
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Header with glassmorphism
                GlassmorphicHeader(
                    freeMinutesLeft = freeMinutesLeft,
                    freeQuotaMinutes = freeQuotaMinutes,
                    onGoToHistory = onGoToHistory
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Animated title
                AnimatedTitle()

                Spacer(modifier = Modifier.height(20.dp))

                // Primary action grid — one glance, one tap.
                // Collapses the old two-parallel-mode-selector design (cleaning preset row +
                // processing mode row + separate MakeReelBanner) into one clear CTA per action.
                ActionGrid(
                    onMakeReel = {
                        onProcessingModeChange(0)
                        onProminentReelClick()
                    },
                    onQuickClean = {
                        onProcessingModeChange(1)
                        onUploadFile()
                    },
                    onAddVibe = {
                        // Show the vibe sheet FIRST — picking a vibe before the file
                        // makes the flow one clear step instead of "upload then scroll
                        // down to find the vibe picker".
                        showVibeSheet = true
                    },
                    onVideoClean = {
                        // Single atomic callback: MainActivity sets
                        // processingFlow=VIDEO_PROCESS and launches the video
                        // picker in one method. Avoids the timing bug where
                        // onUploadFile() read a stale processingFlow.
                        onVideoUpload()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                ModePresetStrip(onPick = onPickModePreset)

                if (!modeAvailabilityNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = modeAvailabilityNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }

                if (showBackgroundControls) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibe: ${backgroundLabels.getOrNull(selectedBackgroundIndex) ?: "—"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentPurple
                        )
                        TextButton(onClick = { showVibeSheet = true }) {
                            Text("Change", color = AccentPurple, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (!adaptiveAnalysisLoading && adaptiveProfile != null) {
                        val vibeHint = VibeUi.suggestedVibeLine(adaptiveProfile)
                        if (vibeHint != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vibeHint,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vibe intensity  ${"%.0f".format(bgMixVolume * 100f)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Slider(
                        value = bgMixVolume,
                        onValueChange = onBgMixVolumeChange,
                        valueRange = 0.05f..0.45f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                HeroCleanPanel(
                    isRecording = isRecording,
                    cleaningPreset = cleaningPreset,
                    helperText = if (isRecording) "Recording… tap stop when done" else "Record with real-time noise cleaning",
                    onCleanTap = {
                        if (isRecording) onStopRecording() else onStartRecording()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (reelVariantFiles.isNotEmpty()) {
                    ReelVariantsCard(
                        reelVariantFiles = reelVariantFiles,
                        onPlay = onPlayReelVariant,
                        onShare = onShareReelVariant,
                        onDownload = onDownloadReelVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Recent cleans header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Cleans",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onGoToHistory) {
                        Text(
                            "See All →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (audioPairs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No recordings yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Record live or pick a file to clean your audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(audioPairs.take(3), key = { it.timestamp }) { pair ->
                val index = audioPairs.indexOf(pair)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInFromBottom() + fadeIn(
                        animationSpec = tween(
                            durationMillis = MD3Motion.StandardDurationMs,
                            delayMillis = index * 100
                        )
                    ),
                    exit = slideOutToBottom() + fadeOut()
                ) {
                    ComparisonCard(
                        pair = pair,
                        recordingsDir = recordingsDir,
                        currentlyPlaying = currentlyPlaying,
                        onPlayOriginal = { 
                            if (currentlyPlaying == pair.originalFile) {
                                onStopFile()
                            } else {
                                onPlayFile(pair.originalFile)
                            }
                        },
                        onPlayCleaned = { 
                            if (currentlyPlaying == pair.cleanedFile) {
                                onStopFile()
                            } else {
                                onPlayFile(pair.cleanedFile)
                            }
                        },
                        onRemove = { onRemovePair(pair.timestamp) },
                        onShare = { onShareFile(pair.cleanedFile) },
                        onDownload = { onDownloadFile(pair.cleanedFile) },
                        onFeedback = { feedbackPair = pair },
                        onMakeVideo = { waveformStylePickerPair = pair },
                        onShareBeforeAfter = { beforeAfterPickerPair = pair },
                        onTightenSilences = { startTightenSilences(pair) },
                        onApplyVoiceStyle = { voiceStylePickerPair = pair },
                        onTrimFillers = { startTrimFillers(pair) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Footer actions row — Batch (Pro) + Settings.
                // The old Upload card was redundant with the ActionGrid above.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Edit,
                        title = "Batch",
                        subtitle = "Multiple files",
                        badge = "PRO"
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Settings,
                        title = "Settings",
                        subtitle = "Customize",
                        onClick = onOpenSettings
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats section
                StatsSection()

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        
        // Bottom navigation bar
        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        // Auto-dismiss after 3s so the banner doesn't stick forever.
        LaunchedEffect(showSuccess) {
            if (showSuccess) {
                kotlinx.coroutines.delay(3000L)
                onDismissSuccess()
            }
        }
        AnimatedVisibility(
            visible = showSuccess && lastSaveInfo != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            lastSaveInfo?.let {
                Box(modifier = Modifier.clickable { onDismissSuccess() }) {
                    SuccessCleanBanner(it)
                }
            }
        }

        // Upload Progress Dialog
        if (isUploading) {
            UploadProgressDialog(
                progress = uploadProgress,
                fileName = selectedFileName ?: "File",
                onCancel = onCancelUpload
            )
        }
        
        // Clean Button Dialog (after upload)
        if (showCleanButton && !isCleaning) {
            CleanFileDialog(
                fileName = selectedFileName ?: "File",
                primaryButtonLabel = processPrimaryButtonLabel,
                helperText = processHelperText,
                cleaningPreset = cleaningPreset,
                adaptiveProfile = adaptiveProfile,
                adaptiveAnalysisLoading = adaptiveAnalysisLoading,
                onApplyAdaptivePreset = onApplyAdaptivePreset,
                onClean = onCleanFile,
                onCancel = onCancelUpload
            )
        }
        
        // Processing Dialog
        if (isCleaning) {
            ProcessingDialog(
                title = processingDialogTitle,
                message = processingDialogSubtitle
            )
        }

        feedbackPair?.let { pair ->
            FeedbackDialog(
                pair = pair,
                onDismiss = { feedbackPair = null },
                onSubmit = { clearVoice, issueType, issueTs, notes ->
                    onSubmitFeedback(pair, clearVoice, issueType, issueTs, notes)
                    feedbackPair = null
                }
            )
        }

        if (showVibeSheet) {
            VibePickerSheet(
                vibes = backgroundLabels,
                selectedIndex = selectedBackgroundIndex.coerceIn(0, backgroundLabels.lastIndex.coerceAtLeast(0)),
                onDismiss = { showVibeSheet = false },
                onPicked = { idx ->
                    onBackgroundIndexChange(idx)
                    showVibeSheet = false
                    // Now that the vibe is chosen, start the add-background flow.
                    onProcessingModeChange(2)
                    onUploadFile()
                }
            )
        }

        videoExportProgress?.let { frac ->
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Making your video…",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { frac.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(frac * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        videoExportError?.let { msg ->
            Dialog(onDismissRequest = { videoExportError = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Video export failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { videoExportError = null },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("OK") }
                    }
                }
            }
        }

        if (beforeAfterInFlight) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Cooking your Before / After…",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val frac = beforeAfterProgress
                        if (frac != null) {
                            LinearProgressIndicator(
                                progress = { frac.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${(frac * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        if (tightenInFlight) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tightening pauses…",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
        }

        tightenMessage?.let { msg ->
            Dialog(onDismissRequest = { tightenMessage = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Silence cutter",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { tightenMessage = null },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("OK") }
                    }
                }
            }
        }

        voiceStylePickerPair?.let { pair ->
            VoiceStyleSheet(
                onDismiss = { voiceStylePickerPair = null },
                onPick = { style ->
                    voiceStylePickerPair = null
                    startApplyVoiceStyle(pair, style)
                }
            )
        }

        if (voiceStyleInFlight) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Applying style…",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
        }

        voiceStyleMessage?.let { msg ->
            Dialog(onDismissRequest = { voiceStyleMessage = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Voice style",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { voiceStyleMessage = null },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("OK") }
                    }
                }
            }
        }

        if (fillerInFlight) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scanning for fillers…",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
        }

        fillerMessage?.let { msg ->
            Dialog(onDismissRequest = { fillerMessage = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Filler trim (beta)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { fillerMessage = null },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("OK") }
                    }
                }
            }
        }

        waveformStylePickerPair?.let { pair ->
            ReelTemplateSheet(
                onDismiss = { waveformStylePickerPair = null },
                onPickTemplate = { template ->
                    waveformStylePickerPair = null
                    startWaveformExport(pair, photoUri = null, template = template)
                },
                onPickPhoto = {
                    waveformStylePickerPair = null
                    pendingPhotoWaveformPair = pair
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            )
        }

        beforeAfterPickerPair?.let { pair ->
            BeforeAfterFormatPicker(
                onDismiss = { beforeAfterPickerPair = null },
                onPickImage9x16 = {
                    beforeAfterPickerPair = null
                    startBeforeAfterImageExport(pair, BeforeAfterImageGenerator.Aspect.PORTRAIT_9_16)
                },
                onPickImage1x1 = {
                    beforeAfterPickerPair = null
                    startBeforeAfterImageExport(pair, BeforeAfterImageGenerator.Aspect.SQUARE_1_1)
                },
                onPickVideo = {
                    beforeAfterPickerPair = null
                    startBeforeAfterVideoExport(pair)
                }
            )
        }

        beforeAfterError?.let { msg ->
            Dialog(onDismissRequest = { beforeAfterError = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Couldn't build share card",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { beforeAfterError = null },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("OK") }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ActionGrid: the primary "what do you want to do?" hub on Home.
// Design notes (2026-04-14):
// - Replaces two competing mode-selector chip rows + a separate "Make Reel"
//   banner that the UX audit flagged as confusing on first visit.
// - Single highlighted hero card (Make Reel) + three secondary cards.
// - Each card self-describes: icon + title + 1-line hint. No cryptic labels
//   like "BG" or "Quick".
// ---------------------------------------------------------------------------
@Composable
fun ActionGrid(
    onMakeReel: () -> Unit,
    onQuickClean: () -> Unit,
    onAddVibe: () -> Unit,
    onVideoClean: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Primary row — 3 distinct gradient tiles.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CheckCircle,
                title = "Quick Clean",
                hint = "Remove noise",
                accentGradient = Brush.linearGradient(listOf(ThemeBlue, ThemeBlueLight)),
                accentColor = ThemeBlue,
                onClick = onQuickClean
            )
            SecondaryActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Add,
                title = "Add Vibe",
                hint = "Rain, cafe, ocean",
                accentGradient = Brush.linearGradient(listOf(AccentPurple, ThemeRed)),
                accentColor = AccentPurple,
                onClick = onAddVibe
            )
            SecondaryActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.PlayArrow,
                title = "Video Clean",
                hint = "Clean in video",
                accentGradient = Brush.linearGradient(listOf(ThemeBlue, AccentCyan)),
                accentColor = AccentCyan,
                onClick = onVideoClean
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Slim Make Reel banner below the primary cards.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CtaOrangeRedGradient)
                .clickable(onClick = onMakeReel)
                .semantics { },
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Make Reel — one tap → clean + vibe + loudness",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SecondaryActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    hint: String,
    accentGradient: Brush,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 112.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = BackgroundCard,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Gradient icon tile
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GlassmorphicHeader(
    freeMinutesLeft: Int,
    freeQuotaMinutes: Int = 20,
    onGoToHistory: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "ReelVoice",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Audio & Video Studio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val safeQuota = freeQuotaMinutes.coerceAtLeast(1)
                val leftFrac = (freeMinutesLeft.coerceAtLeast(0) / safeQuota.toFloat())
                    .coerceIn(0f, 1f)
                val isLow = freeMinutesLeft <= 3
                val chipBg = if (isLow) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
                val chipAccent = if (isLow) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                val chipText = if (isLow) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
                Surface(
                    color = chipBg,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = leftFrac,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = chipAccent,
                                trackColor = chipAccent.copy(alpha = 0.22f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$freeMinutesLeft / $freeQuotaMinutes min",
                            style = MaterialTheme.typography.labelSmall,
                            color = chipText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedTitle() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInFromStart() + fadeIn(
                animationSpec = tween(MD3Motion.EmphasizedDurationMs)
            ),
            exit = slideOutToBottom() + fadeOut()
        ) {
            Text(
                "Studio-grade voice",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedVisibility(
            visible = true,
            enter = slideInFromStart() + fadeIn(
                animationSpec = tween(MD3Motion.StandardDurationMs, delayMillis = 100)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " + ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun HeroCleanPanel(
    isRecording: Boolean,
    cleaningPreset: CleaningPreset,
    helperText: String,
    onCleanTap: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = BackgroundCard,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, SliderTrackStrong.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎙️ Record live",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Surface(
                    color = SurfaceStripe,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = cleaningPreset.label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ThemeBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "On-device RNNoise, no upload needed",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isRecording) {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    )
                    .clickable { onCleanTap() }
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Close else Icons.Filled.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRecording) "Stop Recording" else "Start Recording",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (isRecording) "Your voice is being captured" else helperText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    imageVector: ImageVector? = null,
    title: String,
    subtitle: String,
    badge: String? = null,
    isLoading: Boolean = false,
    onClick: () -> Unit = {}
) {
    val iconToUse = imageVector ?: icon ?: Icons.Default.Info
    val iconBg = when {
        badge == "PRO" -> MaterialTheme.colorScheme.tertiaryContainer
        isLoading -> SurfaceStripe
        else -> ThemeBlue.copy(alpha = 0.1f)
    }
    val iconTint = when {
        badge == "PRO" -> MaterialTheme.colorScheme.tertiary
        isLoading -> ThemeBlue
        else -> ThemeRed
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundCard)
            .border(1.dp, SliderTrackStrong.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ThemeBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = iconToUse,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) ThemeBlue else TextPrimary
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = "98%",
            label = "Noise Removed",
            icon = Icons.Filled.PlayArrow,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "48000Hz",
            label = "Studio Quality",
            icon = Icons.Filled.Star,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "AI",
            label = "RNNoise Engine",
            icon = Icons.Filled.Build,
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundCard)
            .border(
                width = 1.dp,
                color = SliderTrackStrong.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ThemeBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SuccessCleanBanner(info: SaveInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Cleaned & saved",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    info.cleanedFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    String.format(Locale.US, "%.1f s · %s", info.durationSec, info.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun MiniWaveformStrip(cleanedWav: File, modifier: Modifier = Modifier, barCount: Int = 32) {
    var bars by remember(cleanedWav.path) { mutableStateOf<List<Float>>(emptyList()) }
    LaunchedEffect(cleanedWav.path) {
        bars = withContext(Dispatchers.IO) { WavPreview.loadBars(cleanedWav, barCount) }
    }
    Row(
        modifier = modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        if (bars.isEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = AccentGreen.copy(alpha = 0.5f),
                trackColor = SliderTrack
            )
        } else {
            bars.forEach { h ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height((10 + h * 26).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(AccentGreen.copy(alpha = 0.95f), AccentCyan.copy(alpha = 0.35f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun ComparisonCard(
    pair: AudioPair,
    recordingsDir: File,
    currentlyPlaying: String? = null,
    onPlayOriginal: () -> Unit = {},
    onPlayCleaned: () -> Unit = {},
    onRemove: () -> Unit = {},
    onShare: () -> Unit = {},
    onDownload: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onMakeVideo: () -> Unit = {},
    onShareBeforeAfter: () -> Unit = {},
    onTightenSilences: () -> Unit = {},
    onApplyVoiceStyle: () -> Unit = {},
    onTrimFillers: () -> Unit = {}
) {
    val isOriginalPlaying = currentlyPlaying == pair.originalFile
    val isCleanedPlaying = currentlyPlaying == pair.cleanedFile
    val cleanedPath = File(recordingsDir, pair.cleanedFile)
    val isVideo = pair.cleanedFile.endsWith(".mp4", ignoreCase = true) ||
        pair.cleanedFile.endsWith(".mov", ignoreCase = true) ||
        pair.cleanedFile.endsWith(".mkv", ignoreCase = true)

    val typeLabel = when {
        isVideo -> "VIDEO"
        pair.isRecording -> "LIVE"
        else -> "AUDIO"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCleanedPlaying) 6.dp else 2.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCleanedPlaying) 2.dp else 0.5.dp,
            color = if (isCleanedPlaying) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            com.reelvoice.ui.theme.BrandRed.copy(alpha = 0.10f),
                            com.reelvoice.ui.theme.BrandPurple.copy(alpha = 0.06f),
                            com.reelvoice.ui.theme.BrandBlue.copy(alpha = 0.10f),
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1400f)
                    )
                )
        ) {
            // Primary row: play button + title + meta + delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayCleaned() }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular play / pause button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCleanedPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCleanedPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isCleanedPlaying) "Pause" else "Play",
                        tint = if (isCleanedPlaying) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title + meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isVideo) "Cleaned Video" else "Cleaned Audio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = typeLabel,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp
                            )
                        }
                        Text(
                            text = pair.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (pair.durationSec > 0f) {
                            Text(
                                text = "· ${String.format(Locale.US, "%.1fs", pair.durationSec)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Delete
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Video thumbnail (compact, only for video)
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPlayCleaned() },
                    contentAlignment = Alignment.Center
                ) {
                    VideoThumbnail(file = cleanedPath)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.28f)
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCleanedPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isCleanedPlaying) "Pause video" else "Play video",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                // Compact full-width waveform; if an original exists, a small
                // "Play original" chip sits below so the A/B compare stays
                // discoverable without eating a slot in the main action row.
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    )
                ) {
                    MiniWaveformStrip(cleanedPath, modifier = Modifier.fillMaxWidth())
                    if (pair.originalFile != pair.cleanedFile) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPlayOriginal() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOriginalPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isOriginalPlaying) "Stop original" else "Play original",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOriginalPlaying) "Stop original" else "Compare with original",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Compact action grid — fixed slots, SpaceBetween so layout is even
            // on both audio (6 actions) and video (3 actions) cards. Secondary
            // utilities (Save, Feedback) live under a "More" overflow menu.
            var moreExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InlineAction(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    onClick = onShare,
                    tint = MaterialTheme.colorScheme.primary
                )
                if (!isVideo && pair.originalFile != pair.cleanedFile) {
                    InlineAction(
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        label = "B/A",
                        onClick = onShareBeforeAfter,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                if (!isVideo) {
                    InlineAction(
                        icon = Icons.Filled.Movie,
                        label = "Video",
                        onClick = onMakeVideo,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    InlineAction(
                        icon = Icons.Filled.ContentCut,
                        label = "Tighten",
                        onClick = onTightenSilences,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    InlineAction(
                        icon = Icons.Filled.Tune,
                        label = "Style",
                        onClick = onApplyVoiceStyle,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Box {
                    InlineAction(
                        icon = Icons.Filled.MoreHoriz,
                        label = "More",
                        onClick = { moreExpanded = true },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = moreExpanded,
                        onDismissRequest = { moreExpanded = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Save to Downloads") },
                            leadingIcon = {
                                Icon(Icons.Filled.Download, contentDescription = null)
                            },
                            onClick = {
                                moreExpanded = false
                                onDownload()
                            }
                        )
                        if (!isVideo) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Trim fillers (beta)") },
                                leadingIcon = {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                },
                                onClick = {
                                    moreExpanded = false
                                    onTrimFillers()
                                }
                            )
                        }
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Feedback") },
                            leadingIcon = {
                                Icon(Icons.Filled.ThumbUp, contentDescription = null)
                            },
                            onClick = {
                                moreExpanded = false
                                onFeedback()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompactActionIcon(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun VibePickerSheet(
    vibes: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onPicked: (Int) -> Unit
) {
    // Vibe-label → emoji mapping (fallback: 🎵). Kept tiny so any label flows through.
    fun emojiFor(label: String): String {
        val l = label.lowercase(Locale.getDefault())
        return when {
            "rain" in l -> "🌧️"
            "cafe" in l || "coffee" in l -> "☕"
            "ocean" in l || "sea" in l || "wave" in l -> "🌊"
            "street" in l || "city" in l || "traffic" in l -> "🏙️"
            "forest" in l || "jungle" in l -> "🌳"
            "night" in l -> "🌙"
            "fire" in l -> "🔥"
            "wind" in l -> "🌬️"
            "crowd" in l || "party" in l -> "👥"
            "bird" in l -> "🐦"
            else -> "🎵"
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* swallow bg taps inside sheet */ },
                color = BackgroundCard,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Grab handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TextSecondary.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose a vibe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We'll mix it with your voice after you pick a file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (vibes.isEmpty()) {
                        Text(
                            text = "Loading vibes…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        // 2-column flow: chunk into rows of 2.
                        vibes.chunked(2).forEachIndexed { rowIdx, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEachIndexed { colIdx, label ->
                                    val idx = rowIdx * 2 + colIdx
                                    val isSelected = idx == selectedIndex
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 80.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onPicked(idx) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) AccentPurple.copy(alpha = 0.12f) else SurfaceStripe,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) AccentPurple else SliderTrackStrong.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = emojiFor(label),
                                                fontSize = 26.sp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) AccentPurple else TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = AccentPurple,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(file: File) {
    var bmp by remember(file.absolutePath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(file.absolutePath) {
        bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val r = android.media.MediaMetadataRetriever()
                r.setDataSource(file.absolutePath)
                val frame = r.getFrameAtTime(1_000_000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                r.release()
                frame
            }.getOrNull()
        }
    }
    bmp?.let {
        androidx.compose.foundation.Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Video thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}

@Composable
private fun VideoPreviewBox(
    fileName: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onOpen() }
            .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = AccentCyan.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentCyan.copy(alpha = 0.18f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Open video",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cleaned Video",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Open",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
        }
    }
}

@Composable
fun VoiceActionPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    tint: Color = AccentGreen,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceStripe,
        modifier = modifier
            .border(1.dp, SliderTrackStrong.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReelVariantsCard(
    reelVariantFiles: Map<String, String>,
    onPlay: (String) -> Unit,
    onShare: (String) -> Unit,
    onDownload: (String) -> Unit
) {
    val order = listOf("viral_boosted", "with_bg", "clean_only")
    val labels = mapOf(
        "viral_boosted" to "Viral Boosted",
        "with_bg" to "With BG",
        "clean_only" to "Clean Only"
    )
    val available = order.mapNotNull { key -> reelVariantFiles[key]?.let { key to it } }
    if (available.isEmpty()) return

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = BackgroundCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Reel Outputs Ready",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick variant and play/share quickly",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            available.forEachIndexed { idx, (key, fileName) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceStripe
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = labels[key] ?: key,
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = fileName,
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VoiceActionPill(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.PlayArrow,
                                label = "Play",
                                onClick = { onPlay(fileName) }
                            )
                            VoiceActionPill(
                                modifier = Modifier.weight(1f),
                                icon = Icons.AutoMirrored.Filled.Send,
                                label = "Share",
                                onClick = { onShare(fileName) }
                            )
                            VoiceActionPill(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.CheckCircle,
                                label = "Save",
                                onClick = { onDownload(fileName) }
                            )
                        }
                    }
                }
                if (idx != available.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AudioSideBox(
    modifier: Modifier = Modifier,
    title: String,
    fileName: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    borderColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceStripe)
            .border(
                width = if (isPlaying) 2.dp else 1.dp,
                color = if (isPlaying) AccentGreen else borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title badge
        Surface(
            color = if (title.contains("Original"))
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (title.contains("Original")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Play/Stop Button - Larger and more visible
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isPlaying)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                .clickable { onPlay() },
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                // Pause icon (two vertical bars) - Larger
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(20.dp)
                            .background(Color.White)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(20.dp)
                            .background(Color.White)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            } else {
                // Play icon - Larger triangle
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Status text
        Text(
            text = if (isPlaying) "Playing..." else "Tap to play",
            style = MaterialTheme.typography.labelSmall,
            color = if (isPlaying) AccentGreen else TextSecondary
        )
    }
}

// BottomNavBar + NavItem moved to ui/components/BottomNavBar.kt (2026-04-14)
// — shared by HomeScreen, LiveScreen, HistoryScreen, ProfileScreen.

@Composable
fun AudioWaveformAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val time = System.currentTimeMillis() / 1000f
        
        // Draw animated circles
        for (i in 0..3) {
            val radius = 100f + i * 80f + (time * 50f) % 80f
            val alpha = (1f - (radius - 100f) / 320f).coerceIn(0f, 0.3f)
            
            drawCircle(
                color = ThemeBlue.copy(alpha = alpha),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
        }
    }
}

data class CleanItem(
    val name: String,
    val status: String,
    val time: String,
    val originalFile: String? = null,
    val cleanedFile: String? = null,
    val isPair: Boolean = false
)

data class AudioPair(
    val timestamp: String,
    val time: String,
    val originalFile: String,
    val cleanedFile: String,
    val isRecording: Boolean = false,  // true = recording, false = upload
    val durationSec: Float = 0f
)

@Composable
fun FeedbackDialog(
    pair: AudioPair,
    onDismiss: () -> Unit,
    onSubmit: (Boolean, String?, String?, String?) -> Unit
) {
    val issueOptions = listOf("Quiet", "Artifacts", "Noise bacha", "Other")
    var clearVoice by remember(pair.timestamp) { mutableStateOf(true) }
    var issueType by remember(pair.timestamp) { mutableStateOf(issueOptions[0]) }
    var issueTimestamp by remember(pair.timestamp) { mutableStateOf("") }
    var notes by remember(pair.timestamp) { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick feedback", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    pair.cleanedFile,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Voice clear hui?", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = clearVoice, onClick = { clearVoice = true }, label = { Text("Yes") })
                    FilterChip(selected = !clearVoice, onClick = { clearVoice = false }, label = { Text("No") })
                }

                if (!clearVoice) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Issue type", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        issueOptions.forEach { option ->
                            FilterChip(
                                selected = issueType == option,
                                onClick = { issueType = option },
                                label = { Text(option) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = issueTimestamp,
                        onValueChange = { issueTimestamp = it },
                        label = { Text("Issue timestamp (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSubmit(
                                clearVoice,
                                if (clearVoice) null else issueType,
                                issueTimestamp,
                                notes
                            )
                        }
                    ) { Text("Send") }
                }
            }
        }
    }
}

@Composable
fun UploadProgressDialog(
    progress: Int,
    fileName: String,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated upload icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = AccentCyan,
                        strokeWidth = 3.dp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Uploading File...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AccentGreen,
                    trackColor = SliderTrack
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "$progress%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Cancel button
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancel Upload",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedPresetChipRow(
    profile: AdaptiveAudioAnalyzer.Profile?,
    loading: Boolean,
    currentPreset: CleaningPreset,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceStripe,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = AccentCyan,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Checking levels for preset hint…",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            profile?.let { p ->
                Text(
                    text = p.uiHeadline(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud mode: ${p.suggestedCloudMode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentCyan
                        )
                        Text(
                            text = p.flags.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 2
                        )
                    }
                    if (currentPreset != p.suggestedCleaningPreset) {
                        TextButton(onClick = onApply) {
                            Text("Apply", color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "✓ Current",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CleanFileDialog(
    fileName: String,
    primaryButtonLabel: String = "Clean Audio",
    helperText: String = "Tap 'Clean Audio' to remove noise and enhance your audio file",
    cleaningPreset: CleaningPreset = CleaningPreset.NORMAL,
    adaptiveProfile: AdaptiveAudioAnalyzer.Profile? = null,
    adaptiveAnalysisLoading: Boolean = false,
    onApplyAdaptivePreset: () -> Unit = {},
    onClean: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "File Ready!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                if (adaptiveAnalysisLoading || adaptiveProfile != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SuggestedPresetChipRow(
                        profile = adaptiveProfile,
                        loading = adaptiveAnalysisLoading,
                        currentPreset = cleaningPreset,
                        onApply = onApplyAdaptivePreset,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Clean button
                Button(
                    onClick = onClean,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        primaryButtonLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Cancel button
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancel",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessingDialog(
    title: String = "Cleaning Audio...",
    message: String = "AI is removing noise and enhancing your audio. Please wait..."
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated processing indicator
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AccentPurple.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = AccentPurple,
                        strokeWidth = 4.dp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MakeReelBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandGradient.MakeReel),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sparkle icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", style = MaterialTheme.typography.titleMedium)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Make Reel — recommended",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Reel ready in 1 tap",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Arrow indicator
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onClearCache: () -> Unit = {},
    onAbout: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTermsOfService: () -> Unit = {},
    onContactSupport: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customize your experience",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Settings options
                SettingsOption(
                    icon = Icons.Filled.Info,
                    title = "About ReelVoice",
                    subtitle = "Version 1.0.1",
                    onClick = onAbout
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsOption(
                    icon = Icons.Filled.Warning,
                    title = "Clear Cache",
                    subtitle = "Free up storage space",
                    onClick = onClearCache
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsOption(
                    icon = Icons.Filled.Person,
                    title = "Privacy Policy",
                    subtitle = "How we protect your data",
                    onClick = onPrivacyPolicy
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsOption(
                    icon = Icons.Filled.Star,
                    title = "Terms of Service",
                    subtitle = "App usage terms",
                    onClick = onTermsOfService
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsOption(
                    icon = Icons.Filled.Email,
                    title = "Contact Support",
                    subtitle = "ss.sunil9255@gmail.com",
                    onClick = onContactSupport
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Close",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = SurfaceStripe,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ThemeBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
