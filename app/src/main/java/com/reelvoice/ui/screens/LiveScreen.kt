package com.reelvoice.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import com.reelvoice.audio.CleaningPreset
import com.reelvoice.ui.components.BottomNavBar
import com.reelvoice.ui.components.BottomNavBarBottomPadding

/**
 * Live Recording Screen with real-time audio visualization
 */
@Composable
fun LiveScreen(
    isRecording: Boolean = false,
    audioLevel: Float = 0.5f,
    selectedTab: Int = 1,
    freeMinutesLeft: Int = 8,
    audioPairs: List<AudioPair> = emptyList(),
    currentlyPlaying: String? = null,
    cleaningPreset: CleaningPreset = CleaningPreset.NORMAL,
    showSuccess: Boolean = false,
    onDismissSuccess: () -> Unit = {},
    lastSaveInfo: SaveInfo? = null,
    playbackPositionMs: Int = 0,
    playbackDurationMs: Int = 0,
    modeAvailabilityNote: String? = null,
    isVideoMode: Boolean = false,
    onToggleFormat: (Boolean) -> Unit = {},
    onStartVideoRecording: () -> Unit = {},
    onCleaningPresetChange: (CleaningPreset) -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onPlayFile: (String) -> Unit = {},
    onStopFile: () -> Unit = {},
    onRemovePair: (String) -> Unit = {},
    onShareFile: (String) -> Unit = {},
    onDownloadFile: (String) -> Unit = {},
    onSubmitFeedback: (AudioPair, Boolean, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onGoBack: () -> Unit = {}
) {
    val modes = listOf("Normal", "Strong", "Studio")
    var feedbackPair by remember { mutableStateOf<AudioPair?>(null) }

    // Newest recording first — surfaced at the top so a just-cleaned clip never "disappears".
    val latestRecording = remember(audioPairs) {
        audioPairs.filter { it.isRecording }.maxByOrNull { it.timestamp }
    }

    // Auto-dismiss the success banner (matches Home) so it doesn't stick forever.
    LaunchedEffect(showSuccess, lastSaveInfo?.cleanedFileName) {
        if (showSuccess) {
            kotlinx.coroutines.delay(4000L)
            onDismissSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle solid overlay to keep depth without gradients
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
                .height(250.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0f),
                        radius = 0.5f
                    )
                )
        )
        
        // Top Header
        GlassmorphicHeader(
            freeMinutesLeft = freeMinutesLeft,
            onGoToHistory = { onTabSelected(2) }
        )
        
        // Main content: scrollable body + docked record footer (button never clipped behind the nav)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
                .padding(bottom = BottomNavBarBottomPadding + 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable content region — scrolls inside the space above the docked footer
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Just-recorded result — the cleaned clip stays visible + playable here,
            // instead of only living far down the scroll (the "where did it go?" bug).
            AnimatedVisibility(
                visible = showSuccess && lastSaveInfo != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                lastSaveInfo?.let { info ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        SuccessCleanBanner(info)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onDismissSuccess() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Dismiss") }
                            Button(
                                onClick = {
                                    onPlayFile(info.cleanedFileName)
                                    onDismissSuccess()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Play cleaned") }
                        }
                    }
                }
            }

            // Persistent latest-recording card: Original ⟷ Cleaned + live progress.
            if (!isRecording && latestRecording != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LiveLatestRecordingCard(
                    pair = latestRecording,
                    currentlyPlaying = currentlyPlaying,
                    playbackPositionMs = playbackPositionMs,
                    playbackDurationMs = playbackDurationMs,
                    onPlayOriginal = { onPlayFile(latestRecording.originalFile) },
                    onPlayCleaned = { onPlayFile(latestRecording.cleanedFile) },
                    onShare = { onShareFile(latestRecording.cleanedFile) },
                    onDownload = { onDownloadFile(latestRecording.cleanedFile) },
                    onFeedback = { feedbackPair = latestRecording }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Format toggle: Audio ⟷ Video
            if (!isRecording) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FormatToggleChip(
                        icon = Icons.Filled.Mic,
                        label = "Audio",
                        isSelected = !isVideoMode,
                        onClick = { onToggleFormat(false) }
                    )
                    FormatToggleChip(
                        icon = Icons.Filled.Videocam,
                        label = "Video",
                        isSelected = isVideoMode,
                        onClick = { onToggleFormat(true) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main visualization area with microphone — compact single control.
            // Tapping the circle itself starts/stops (no duplicate red button below).
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .clickable {
                        when {
                            isRecording -> onStopRecording()
                            isVideoMode -> onStartVideoRecording()
                            else -> onStartRecording()
                        }
                    }
                    .semantics {
                        contentDescription = if (isRecording) "Stop recording" else "Start recording"
                    },
                contentAlignment = Alignment.Center
            ) {
                // Animated waveform bars behind microphone
                if (isRecording) {
                    AudioWaveformVisualization(
                        modifier = Modifier.fillMaxSize(),
                        audioLevel = audioLevel,
                        isRecording = isRecording
                    )
                }
                
                // Glowing circles
                repeat(3) { index ->
                    val delay = index * 300
                    val animatedScale by rememberInfiniteTransition(label = "ring_$index").animateFloat(
                        initialValue = 1f,
                        targetValue = 1.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, delayMillis = delay, easing = EaseOutCubic),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ring_$index"
                    )
                    val alpha = (1.8f - animatedScale).coerceIn(0f, 0.5f)
                    
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(animatedScale)
                            .alpha(alpha)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        if (isRecording) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }
                
                // Main circle border
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(
                            width = 3.dp,
                            brush = if (isRecording) {
                                Brush.sweepGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.error,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.error
                                    )
                                )
                            } else {
                                Brush.sweepGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                                )
                            },
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.MicNone,
                        contentDescription = if (isRecording) "Stop recording" else "Start recording",
                        modifier = Modifier.size(80.dp),
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
                
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats bar
            StatsBar(
                isRecording = isRecording,
                noiseReduction = 68,
                clarityBoost = 42,
                latency = 60
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Mode selector
            ModeSelector(
                modes = modes,
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recent Recordings Section
            if (audioPairs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Section title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Recordings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (audioPairs.size == 1) "1 file" else "${audioPairs.size} files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show only recording files (not uploaded)
                audioPairs.filter { it.isRecording }.take(3).forEach { pair ->
                    LiveRecordingCard(
                        pair = pair,
                        currentlyPlaying = currentlyPlaying,
                        playbackPositionMs = playbackPositionMs,
                        playbackDurationMs = playbackDurationMs,
                        onPlayOriginal = { onPlayFile(pair.originalFile) },
                        onPlayCleaned = { onPlayFile(pair.cleanedFile) },
                        onRemove = { onRemovePair(pair.timestamp) },
                        onShare = { onShareFile(pair.cleanedFile) },
                        onDownload = { onDownloadFile(pair.cleanedFile) },
                        onFeedback = { feedbackPair = pair }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            }  // end scrollable content region

            // Docked footer: status + hint, always visible above the nav bar.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = if (isRecording) "Recording live" else "Ready to record"
                    }
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        if (isRecording) "LIVE CLEANING" else "Ready — tap the mic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Headphones recommended — less echo, cleaner preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    if (isVideoMode) "Tap the circle to record video" else "Tap the mic to start / stop",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
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

        // Bottom navigation bar
        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun FormatToggleChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsBar(
    isRecording: Boolean,
    noiseReduction: Int,
    clarityBoost: Int,
    latency: Int
) {
    val animatedNoise by animateFloatAsState(
        targetValue = if (isRecording) noiseReduction.toFloat() else 0f,
        animationSpec = tween(500),
        label = "noise"
    )
    val animatedClarity by animateFloatAsState(
        targetValue = if (isRecording) clarityBoost.toFloat() else 0f,
        animationSpec = tween(500),
        label = "clarity"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    icon = Icons.Filled.Build,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    label = "Noise",
                    value = "↓ ${animatedNoise.toInt()}%",
                    isActive = isRecording
                )

                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                StatItem(
                    icon = Icons.Default.Star,
                    iconColor = MaterialTheme.colorScheme.primary,
                    label = "Clarity",
                    value = "↑ ${animatedClarity.toInt()}%",
                    isActive = isRecording
                )

                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                StatItem(
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    label = "Latency",
                    value = "${latency}ms",
                    isActive = true
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) iconColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ModeSelector(
    modes: List<String>,
    selectedIndex: Int,
    onModeSelected: (Int) -> Unit
) {
    val scroll = rememberScrollState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(30.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(30.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(scroll)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                modes.forEachIndexed { index, mode ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .widthIn(min = 76.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                brush = if (isSelected) {
                                    Brush.horizontalGradient(colors = listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primary))
                                } else {
                                    Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                },
                                shape = RoundedCornerShape(26.dp)
                            )
                            .then(
                                if (!isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(26.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onModeSelected(index) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            mode,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioWaveformVisualization(
    modifier: Modifier = Modifier,
    audioLevel: Float,
    isRecording: Boolean
) {
    val barCount = 40
    val random = remember { Random(System.currentTimeMillis()) }
    
    // Generate animated bars
    val barHeights = remember { List(barCount) { random.nextFloat() * 0.8f + 0.2f } }
    
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side bars
        barHeights.take(barCount / 2).asReversed().forEachIndexed { index, baseHeight ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = baseHeight * 0.5f,
                targetValue = baseHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 300 + index * 20,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_left_$index"
            )
            
            WaveformBar(
                height = if (isRecording) animatedHeight * audioLevel else 0.1f,
                delay = index * 2
            )
        }
        
        Spacer(modifier = Modifier.width(60.dp)) // Space for microphone
        
        // Right side bars
        barHeights.take(barCount / 2).forEachIndexed { index, baseHeight ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = baseHeight * 0.5f,
                targetValue = baseHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 300 + index * 20,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_right_$index"
            )
            
            WaveformBar(
                height = if (isRecording) animatedHeight * audioLevel else 0.1f,
                delay = index * 2
            )
        }
    }
}

@Composable
fun WaveformBar(
    height: Float,
    delay: Int
) {
    val color = when {
        delay % 3 == 0 -> MaterialTheme.colorScheme.primary
        delay % 3 == 1 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    
    Box(
        modifier = Modifier
            .width(4.dp)
            .height((20 + height * 80).dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        color.copy(alpha = 0.3f)
                    )
                )
            )
    )
}

// Live Recording Card for displaying recorded files on Live screen
@Composable
fun LiveRecordingCard(
    pair: AudioPair,
    currentlyPlaying: String?,
    playbackPositionMs: Int = 0,
    playbackDurationMs: Int = 0,
    onPlayOriginal: () -> Unit,
    onPlayCleaned: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onFeedback: () -> Unit
) {
    val isOriginalPlaying = currentlyPlaying == pair.originalFile
    val isCleanedPlaying = currentlyPlaying == pair.cleanedFile
    val progress = if (playbackDurationMs > 0) {
        playbackPositionMs.toFloat() / playbackDurationMs.toFloat()
    } else 0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
            // Header with timestamp and remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pair.time,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Side by side - Original and Cleaned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Original
                LiveAudioButton(
                    modifier = Modifier.weight(1f),
                    label = "Original",
                    isPlaying = isOriginalPlaying,
                    onClick = onPlayOriginal,
                    borderColor = MaterialTheme.colorScheme.tertiary,
                    progress = if (isOriginalPlaying) progress else null,
                    positionMs = playbackPositionMs,
                    durationMs = playbackDurationMs
                )
                
                // Cleaned
                LiveAudioButton(
                    modifier = Modifier.weight(1f),
                    label = "Cleaned",
                    isPlaying = isCleanedPlaying,
                    onClick = onPlayCleaned,
                    borderColor = MaterialTheme.colorScheme.primary,
                    progress = if (isCleanedPlaying) progress else null,
                    positionMs = playbackPositionMs,
                    durationMs = playbackDurationMs
                )
            }
            
            // Action buttons
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LiveActionButton(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    onClick = onShare
                )

                LiveActionButton(
                    icon = Icons.Filled.Download,
                    label = "Save",
                    onClick = onDownload
                )

                LiveActionButton(
                    icon = Icons.Filled.ThumbUp,
                    label = "Feedback",
                    onClick = onFeedback
                )
            }
        }
    }
}
}

@Composable
fun LiveAudioButton(
    modifier: Modifier = Modifier,
    label: String,
    isPlaying: Boolean,
    onClick: () -> Unit,
    borderColor: Color,
    progress: Float? = null,
    positionMs: Int = 0,
    durationMs: Int = 0
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(
                width = if (isPlaying) 2.dp else 1.dp,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else borderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Play/Stop icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                // Pause bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .background(Color.White)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Live progress bar while this track is playing.
        if (progress != null) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${formatPlaybackTime(positionMs)} / ${formatPlaybackTime(durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatPlaybackTime(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(java.util.Locale.US, m, s)
}

/**
 * Highlights the single most-recent recording right at the top of the Live tab,
 * with Original ⟷ Cleaned playback + a live progress bar. Fixes the "cleaned clip
 * vanished after recording" complaint.
 */
@Composable
fun LiveLatestRecordingCard(
    pair: AudioPair,
    currentlyPlaying: String?,
    playbackPositionMs: Int,
    playbackDurationMs: Int,
    onPlayOriginal: () -> Unit,
    onPlayCleaned: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onFeedback: () -> Unit
) {
    val isOriginalPlaying = currentlyPlaying == pair.originalFile
    val isCleanedPlaying = currentlyPlaying == pair.cleanedFile
    val progress = if (playbackDurationMs > 0) {
        playbackPositionMs.toFloat() / playbackDurationMs.toFloat()
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Latest recording",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    pair.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveAudioButton(
                    modifier = Modifier.weight(1f),
                    label = "Original",
                    isPlaying = isOriginalPlaying,
                    onClick = onPlayOriginal,
                    borderColor = MaterialTheme.colorScheme.tertiary,
                    progress = if (isOriginalPlaying) progress else null,
                    positionMs = playbackPositionMs,
                    durationMs = playbackDurationMs
                )
                LiveAudioButton(
                    modifier = Modifier.weight(1f),
                    label = "Cleaned",
                    isPlaying = isCleanedPlaying,
                    onClick = onPlayCleaned,
                    borderColor = MaterialTheme.colorScheme.primary,
                    progress = if (isCleanedPlaying) progress else null,
                    positionMs = playbackPositionMs,
                    durationMs = playbackDurationMs
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LiveActionButton(icon = Icons.Filled.Share, label = "Share", onClick = onShare)
                LiveActionButton(icon = Icons.Filled.Download, label = "Save", onClick = onDownload)
                LiveActionButton(icon = Icons.Filled.ThumbUp, label = "Feedback", onClick = onFeedback)
            }
        }
    }
}

@Composable
fun LiveActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
