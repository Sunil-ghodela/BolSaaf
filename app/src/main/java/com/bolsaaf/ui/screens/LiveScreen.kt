package com.bolsaaf.ui.screens

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import com.bolsaaf.audio.CleaningPreset
import com.bolsaaf.ui.theme.AccentCyan
import com.bolsaaf.ui.theme.AccentGreen
import com.bolsaaf.ui.theme.AccentPurple
import com.bolsaaf.ui.theme.BackgroundCard
import com.bolsaaf.ui.theme.BackgroundDark
import com.bolsaaf.ui.theme.PanelOverlay
import com.bolsaaf.ui.theme.SliderTrackStrong
import com.bolsaaf.ui.theme.ThemeBlue
import com.bolsaaf.ui.theme.ThemeRed
import com.bolsaaf.ui.theme.SurfaceStripe
import com.bolsaaf.ui.theme.TextPrimary
import com.bolsaaf.ui.theme.TextSecondary

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
    modeAvailabilityNote: String? = null,
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Subtle solid overlay to keep depth without gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCard.copy(alpha = 0.45f))
        )
        
        // Top Header
        GlassmorphicHeader(
            freeMinutesLeft = freeMinutesLeft,
            onGoToHistory = { onTabSelected(2) }
        )
        
        // Main content with scrolling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 80.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Main visualization area with microphone
            Box(
                modifier = Modifier.size(300.dp),
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
                                        if (isRecording) AccentGreen.copy(alpha = 0.6f) else AccentPurple.copy(alpha = 0.4f),
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
                            brush = Brush.sweepGradient(
                                colors = listOf(AccentGreen, AccentCyan, AccentPurple, AccentGreen)
                            ),
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SurfaceStripe,
                                    BackgroundCard
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Microphone icon - using alternative icons since Mic/MicNone not available
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Menu else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Live mic" else "Idle",
                        modifier = Modifier.size(80.dp),
                        tint = if (isRecording) AccentGreen else AccentPurple
                    )
                }
                
                // Recording indicator dot
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 20.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF5350))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Stats bar
            StatsBar(
                isRecording = isRecording,
                noiseReduction = 68,
                clarityBoost = 42,
                latency = 60
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
                    fontSize = 12.sp,
                    color = Color(0xFFFFA726)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isRecording) AccentGreen else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        if (isRecording) "LIVE CLEANING" else "Ready — tap record",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isRecording) AccentGreen else TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Warning text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Headphones recommended — less echo, cleaner preview",
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
            
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "${audioPairs.size} files",
                        fontSize = 12.sp,
                        color = AccentGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show only recording files (not uploaded)
                audioPairs.filter { it.isRecording }.take(3).forEach { pair ->
                    LiveRecordingCard(
                        pair = pair,
                        currentlyPlaying = currentlyPlaying,
                        onPlayOriginal = { onPlayFile(pair.originalFile) },
                        onPlayCleaned = { onPlayFile(pair.cleanedFile) },
                        onRemove = { onRemovePair(pair.timestamp) },
                        onShare = { onShareFile(pair.cleanedFile) },
                        onDownload = { onDownloadFile(pair.cleanedFile) },
                        onFeedback = { feedbackPair = pair }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Recording button
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
            ) {
                // Pulse effect when recording
                if (isRecording) {
                    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .alpha(0.3f)
                            .background(Color(0xFFEF5350), CircleShape)
                            .align(Alignment.Center)
                    )
                }
                
                // Main button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color(0xFFEF5350) else AccentGreen
                        )
                        .clickable {
                            if (isRecording) onStopRecording() else onStartRecording()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Stop live" else "Start live",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
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
                    color = BackgroundCard.copy(alpha = 0.95f),
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
                    .background(PanelOverlay)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    icon = Icons.Filled.Build,
                    iconColor = AccentCyan,
                    label = "Noise",
                    value = "↓ ${animatedNoise.toInt()}%",
                    isActive = isRecording
                )

                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = TextSecondary.copy(alpha = 0.3f)
                )

                StatItem(
                    icon = Icons.Default.Star,
                    iconColor = AccentGreen,
                    label = "Clarity",
                    value = "↑ ${animatedClarity.toInt()}%",
                    isActive = isRecording
                )

                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = TextSecondary.copy(alpha = 0.3f)
                )

                StatItem(
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFFFFA726),
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
                tint = if (isActive) iconColor else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) TextPrimary else TextSecondary.copy(alpha = 0.5f)
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
                    color = BackgroundCard.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(30.dp)
                )
                .border(
                    width = 1.dp,
                    color = SliderTrackStrong.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(BackgroundCard)
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
                                    Brush.horizontalGradient(colors = listOf(ThemeRed, ThemeBlue))
                                } else {
                                    Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                },
                                shape = RoundedCornerShape(26.dp)
                            )
                            .then(
                                if (!isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        SliderTrackStrong.copy(alpha = 0.4f),
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
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary
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
    val color = remember { 
        if (delay % 3 == 0) AccentGreen 
        else if (delay % 3 == 1) AccentCyan 
        else AccentPurple 
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
    onPlayOriginal: () -> Unit,
    onPlayCleaned: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onFeedback: () -> Unit
) {
    val isOriginalPlaying = currentlyPlaying == pair.originalFile
    val isCleanedPlaying = currentlyPlaying == pair.cleanedFile
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = BackgroundCard.copy(alpha = 0.95f),
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
                    .background(PanelOverlay)
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFFEF5350),
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
                    borderColor = Color(0xFFFF9800)
                )
                
                // Cleaned
                LiveAudioButton(
                    modifier = Modifier.weight(1f),
                    label = "Cleaned",
                    isPlaying = isCleanedPlaying,
                    onClick = onPlayCleaned,
                    borderColor = AccentGreen
                )
            }
            
            // Action buttons
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Share
                LiveActionButton(
                    icon = Icons.Filled.PlayArrow,  // Using PlayArrow as share alternative
                    label = "Share",
                    onClick = onShare
                )
                
                // Download
                LiveActionButton(
                    icon = Icons.Filled.Info,
                    label = "Save",
                    onClick = onDownload
                )
                LiveActionButton(
                    icon = Icons.Default.Person,
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
    borderColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundCard.copy(alpha = 0.95f))
            .border(
                width = if (isPlaying) 2.dp else 1.dp,
                color = if (isPlaying) AccentGreen else borderColor.copy(alpha = 0.5f),
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
                .background(AccentPurple.copy(alpha = 0.6f)),
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
            fontSize = 11.sp,
            color = TextPrimary
        )
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
                .background(AccentGreen),
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
            fontSize = 10.sp,
            color = TextPrimary
        )
    }
}
