package com.bolsaaf.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolsaaf.ui.VibeUi
import com.bolsaaf.ui.theme.AccentCyan
import com.bolsaaf.ui.theme.AccentGreen
import com.bolsaaf.ui.theme.AccentPurple
import com.bolsaaf.ui.theme.BackgroundCard
import com.bolsaaf.ui.theme.BackgroundDark
import com.bolsaaf.ui.theme.CtaOrangeRedGradient
import com.bolsaaf.ui.theme.NavUnselected
import com.bolsaaf.ui.theme.PanelOverlay
import com.bolsaaf.ui.theme.PrimaryGradient
import com.bolsaaf.ui.theme.SliderTrack
import com.bolsaaf.ui.theme.SliderTrackStrong
import com.bolsaaf.ui.theme.SubtitleBluePurple
import com.bolsaaf.ui.theme.SurfaceStripe
import com.bolsaaf.ui.theme.TextPrimary
import com.bolsaaf.ui.theme.TextSecondary
import com.bolsaaf.ui.theme.ThemeBlue
import com.bolsaaf.ui.theme.ThemeRed
import com.bolsaaf.ui.theme.TitleVideoAccent
import com.bolsaaf.audio.AdaptiveAudioAnalyzer
import com.bolsaaf.audio.CleaningPreset
import com.bolsaaf.audio.WavPreview
import com.bolsaaf.ui.animation.MD3Motion
import com.bolsaaf.ui.animation.slideInFromStart
import com.bolsaaf.ui.animation.slideInFromBottom
import com.bolsaaf.ui.animation.slideOutToBottom
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    onCleanFile: () -> Unit = {},
    onCancelUpload: () -> Unit = {},
    onGoToHistory: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    audioPairs: List<AudioPair> = emptyList(),
    currentProcessedPair: AudioPair? = null,
    freeMinutesLeft: Int = 8,
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
    onProminentReelClick: () -> Unit = {}
) {
    var feedbackPair by remember { mutableStateOf<AudioPair?>(null) }
    
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
                    onGoToHistory = onGoToHistory
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Animated title
                AnimatedTitle()

                Spacer(modifier = Modifier.height(16.dp))
                val presetModes = listOf("Normal", "Strong", "Studio")
                ModeSelector(
                    modes = presetModes,
                    selectedIndex = cleaningPreset.ordinal,
                    onModeSelected = { onCleaningPresetChange(CleaningPreset.fromIndex(it)) }
                )
                if (!modeAvailabilityNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = modeAvailabilityNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (showCleanButton && (adaptiveAnalysisLoading || adaptiveProfile != null)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SuggestedPresetChipRow(
                        profile = adaptiveProfile,
                        loading = adaptiveAnalysisLoading,
                        currentPreset = cleaningPreset,
                        onApply = onApplyAdaptivePreset,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                ModeSelector(
                    modes = processingModes,
                    selectedIndex = selectedProcessingModeIndex,
                    onModeSelected = onProcessingModeChange
                )

                if (selectedProcessingModeIndex == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✨ 1 tap → reel ready",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onProminentReelClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Make Reel — recommended",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showBackgroundControls) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose Vibe 🌊",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    if (!adaptiveAnalysisLoading && adaptiveProfile != null) {
                        val vibeHint = VibeUi.suggestedVibeLine(adaptiveProfile)
                        if (vibeHint != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vibeHint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (backgroundLabels.isNotEmpty()) {
                        ModeSelector(
                            modes = backgroundLabels,
                            selectedIndex = selectedBackgroundIndex.coerceIn(0, backgroundLabels.lastIndex),
                            onModeSelected = onBackgroundIndexChange
                        )
                    } else {
                        Text(
                            text = "Loading vibes…",
                            fontSize = 12.sp,
                            color = Color(0xFFFFA726),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vibe intensity  ${"%.0f".format(bgMixVolume * 100f)}%",
                        fontSize = 12.sp,
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
                    helperText = if (isRecording) "Recording in progress" else "Tap to clean and polish",
                    onCleanTap = {
                        if (isRecording) onStopRecording() else onStartRecording()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
                
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
                        onFeedback = { feedbackPair = pair }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Quick action cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        imageVector = if (isUploading) Icons.Rounded.Refresh else Icons.AutoMirrored.Filled.Send,
                        title = if (isUploading) "Uploading..." else "Upload",
                        subtitle = if (isUploading) "Processing file..." else "File se clean karo",
                        isLoading = isUploading,
                        onClick = onUploadFile
                    )
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
                        subtitle = "Customize karo"
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

        AnimatedVisibility(
            visible = showSuccess && lastSaveInfo != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            lastSaveInfo?.let { SuccessCleanBanner(it) }
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
    }
}

@Composable
fun GlassmorphicHeader(
    freeMinutesLeft: Int,
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
                        "BolSaaf",
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
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$freeMinutesLeft min free",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                "Studio jaisi awaaz",
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
                    text = "Audio dono",
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
                    text = "🎧 Reel ready voice",
                    fontSize = 20.sp,
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ThemeBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Clean • Balanced • Natural",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CtaOrangeRedGradient)
                    .clickable { onCleanTap() }
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Close else Icons.Filled.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRecording) "Stop Recording" else "Clean Audio / Video",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isRecording) "Your voice is being captured" else helperText,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = helperText,
                fontSize = 12.sp,
                color = TextSecondary
            )
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
        badge == "PRO" -> Color(0xFFFFF3E0)
        isLoading -> SurfaceStripe
        else -> ThemeBlue.copy(alpha = 0.1f)
    }
    val iconTint = when {
        badge == "PRO" -> Color(0xFFFF8F00)
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) ThemeBlue else TextPrimary
        )
        Text(
            subtitle,
            fontSize = 11.sp,
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            label,
            fontSize = 10.sp,
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    info.cleanedFileName,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    String.format(Locale.US, "%.1f s · %s", info.durationSec, info.timestamp),
                    fontSize = 11.sp,
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
    onFeedback: () -> Unit = {}
) {
    val isOriginalPlaying = currentlyPlaying == pair.originalFile
    val isCleanedPlaying = currentlyPlaying == pair.cleanedFile
    val cleanedPath = File(recordingsDir, pair.cleanedFile)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(colors = listOf(ThemeRed, ThemeBlue)))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = pair.time,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    if (pair.durationSec > 0f) {
                        Text(
                            text = String.format(Locale.US, "%.1f s", pair.durationSec),
                            fontSize = 12.sp,
                            color = AccentGreen.copy(alpha = 0.9f)
                        )
                    }
                }
                Surface(
                    color = if (pair.isRecording) AccentPurple.copy(alpha = 0.2f) else BackgroundCard,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (pair.isRecording) "Live" else "Processed",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pair.isRecording) AccentPurple else AccentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MiniWaveformStrip(cleanedPath, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioSideBox(
                    modifier = Modifier.weight(1f),
                    title = "🎤 Original",
                    fileName = pair.originalFile,
                    isPlaying = isOriginalPlaying,
                    onPlay = onPlayOriginal,
                    borderColor = Color(0xFFFF9800).copy(alpha = 0.5f)
                )
                AudioSideBox(
                    modifier = Modifier.weight(1f),
                    title = "✨ Cleaned",
                    fileName = pair.cleanedFile,
                    isPlaying = isCleanedPlaying,
                    onPlay = onPlayCleaned,
                    borderColor = AccentGreen.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VoiceActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Share",
                    onClick = onShare
                )
                VoiceActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CheckCircle,
                    label = "Save",
                    onClick = onDownload
                )
                VoiceActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Info,
                    label = "Feedback",
                    onClick = onFeedback
                )
                VoiceActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Close,
                    label = "Delete",
                    tint = Color(0xFFEF5350),
                    onClick = onRemove
                )
            }
            }
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
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
                Color(0xFFFF9800).copy(alpha = 0.2f) 
            else 
                AccentGreen.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (title.contains("Original")) Color(0xFFFF9800) else AccentGreen,
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
                        Color(0xFFEF5350)
                    else 
                        AccentGreen
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
            fontSize = 9.sp,
            color = if (isPlaying) AccentGreen else TextSecondary
        )
    }
}

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SliderTrackStrong.copy(alpha = 0.35f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = BackgroundCard,
        shadowElevation = 12.dp,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
                NavItem(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                NavItem(
                    icon = Icons.Default.PlayArrow,
                    label = "Live",
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
                NavItem(
                    icon = Icons.Filled.Menu,
                    label = "History",
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
                NavItem(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    isSelected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ThemeRed else NavUnselected,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = if (isSelected) ThemeRed else NavUnselected,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(ThemeRed)
            )
        }
    }
}

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
                Text("Quick feedback", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    pair.cleanedFile,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Voice clear hui?", color = TextSecondary, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = clearVoice, onClick = { clearVoice = true }, label = { Text("Yes") })
                    FilterChip(selected = !clearVoice, onClick = { clearVoice = false }, label = { Text("No") })
                }

                if (!clearVoice) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Issue type", color = TextSecondary, fontSize = 12.sp)
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    fileName,
                    fontSize = 14.sp,
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
                    fontSize = 18.sp,
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
                        color = Color(0xFFEF5350),
                        fontSize = 16.sp
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
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            profile?.let { p ->
                Text(
                    text = p.uiHeadline(),
                    fontSize = 14.sp,
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
                            fontSize = 12.sp,
                            color = AccentCyan
                        )
                        Text(
                            text = p.flags.joinToString(", "),
                            fontSize = 10.sp,
                            color = Color(0xFFFFB74D),
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
                            fontSize = 12.sp,
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    fileName,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    helperText,
                    fontSize = 12.sp,
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
                        fontSize = 16.sp,
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
                        fontSize = 14.sp
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
