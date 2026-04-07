package com.bolsaaf.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolsaaf.audio.CleaningPreset
import com.bolsaaf.audio.WavPreview
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// New Premium Color Scheme
val BackgroundDark = Color(0xFF0A0F1E)      // Deep blue background
val BackgroundCard = Color(0xFF141B2D)      // Card background
val AccentGreen = Color(0xFF00E676)         // Primary accent
val AccentPurple = Color(0xFF9C27B0)        // Purple highlight
val AccentCyan = Color(0xFF00BCD4)          // Cyan secondary
val TextPrimary = Color(0xFFFFFFFF)         // White text
val TextSecondary = Color(0xFF8B95A5)       // Gray text

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
    onDownloadFile: (String) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Animated wave effect for recording state
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )
    
    // Pulse effect for main button
    // Keep record CTA layout stable while recording toggles.
    val pulseScale = 1f
    
    // Rotating gradient animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Animated gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BackgroundDark,
                            BackgroundCard,
                            BackgroundDark
                        )
                    )
                )
        )
        
        // Purple accent glow at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentPurple.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0f),
                        radius = 0.5f
                    )
                )
        )
        
        // Cyan accent glow at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentCyan.copy(alpha = 0.1f),
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

                Spacer(modifier = Modifier.height(32.dp))

                // Main recording button with glow effect
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Keep button visuals stable during record toggles (no expanding rings).
                    
                    // Main button with rotating gradient border
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        AccentGreen,
                                        AccentCyan,
                                        AccentPurple,
                                        AccentGreen
                                    ),
                                    center = Offset(0.5f, 0.5f)
                                )
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) 
                                    Brush.radialGradient(
                                        colors = listOf(AccentPurple, Color(0xFF7B1FA2))
                                    )
                                else 
                                    Brush.radialGradient(
                                        colors = listOf(BackgroundCard, BackgroundDark)
                                    )
                            )
                            .clickable { 
                                if (isRecording) onStopRecording() else onStartRecording()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner core (fixed size to prevent jump on click)
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording)
                                        AccentPurple.copy(alpha = 0.3f)
                                    else
                                        AccentGreen.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Filled.Close else Icons.Filled.Add,
                                    contentDescription = if (isRecording) "Stop" else "Record",
                                    tint = if (isRecording) Color.White else AccentGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isRecording) "Recording..." else "Tap to Clean",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isRecording) "Noise clean ho rahi hai" else "Noise hatao, awaaz saaf karo",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = onGoToHistory) {
                        Text(
                            "See All →",
                            color = Color(0xFF00E676),
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            items(audioPairs.take(3)) { pair ->
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
                    onDownload = { onDownloadFile(pair.cleanedFile) }
                )
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
                onClean = onCleanFile,
                onCancel = onCancelUpload
            )
        }
        
        // Processing Dialog
        if (isCleaning) {
            ProcessingDialog()
        }
    }
}

@Composable
fun GlassmorphicHeader(
    freeMinutesLeft: Int,
    onGoToHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF00E676), Color(0xFF00C853))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "BolSaaf",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "AI Voice Cleaner",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF00E676).copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "$freeMinutesLeft min free",
                        fontSize = 12.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "title")
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Studio jaisi awaaz",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Ghar baithe banao",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AccentGreen,
            textAlign = TextAlign.Center
        )
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
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF142414))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (badge == "PRO") 
                        Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF8F00)))
                    else if (isLoading)
                        Brush.linearGradient(listOf(AccentCyan, AccentGreen))
                    else 
                        Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00C853)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = iconToUse,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) AccentCyan else Color.White
        )
        Text(
            subtitle,
            fontSize = 11.sp,
            color = if (isLoading) AccentGreen else Color(0xFF888888),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2E1A))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            fontSize = 10.sp,
            color = Color(0xFF888888)
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
fun MiniWaveformStrip(cleanedWav: File, modifier: Modifier = Modifier) {
    var bars by remember(cleanedWav.path) { mutableStateOf<List<Float>>(emptyList()) }
    LaunchedEffect(cleanedWav.path) {
        bars = withContext(Dispatchers.IO) { WavPreview.loadBars(cleanedWav, 36) }
    }
    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        if (bars.isEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = AccentGreen.copy(alpha = 0.5f),
                trackColor = Color(0xFF1A2540)
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
    onDownload: () -> Unit = {}
) {
    val isOriginalPlaying = currentlyPlaying == pair.originalFile
    val isCleanedPlaying = currentlyPlaying == pair.cleanedFile
    val cleanedPath = File(recordingsDir, pair.cleanedFile)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with timestamp and remove button
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
                    if (pair.durationSec > 0.1f) {
                        Text(
                            String.format(Locale.US, "%.1f s", pair.durationSec),
                            fontSize = 12.sp,
                            color = AccentGreen.copy(alpha = 0.85f)
                        )
                    }
                }

                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            MiniWaveformStrip(cleanedPath, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))

            // Side by side comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ORIGINAL (Left side)
                AudioSideBox(
                    modifier = Modifier.weight(1f),
                    title = "🎤 Original",
                    fileName = pair.originalFile,
                    isPlaying = isOriginalPlaying,
                    onPlay = onPlayOriginal,
                    borderColor = Color(0xFFFF9800).copy(alpha = 0.5f)
                )
                
                // CLEANED (Right side)
                AudioSideBox(
                    modifier = Modifier.weight(1f),
                    title = "✨ Cleaned",
                    fileName = pair.cleanedFile,
                    isPlaying = isCleanedPlaying,
                    onPlay = onPlayCleaned,
                    borderColor = AccentGreen.copy(alpha = 0.5f)
                )
            }
            
            // Action buttons for cleaned file
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Share",
                    onClick = onShare
                )
                ActionButton(
                    icon = Icons.Default.Info,
                    label = "Save",
                    onClick = onDownload
                )
                ActionButton(
                    icon = Icons.Filled.Close,
                    label = "Delete",
                    onClick = onRemove
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AccentGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
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
            .background(Color(0xFF1A2540))
            .border(
                width = if (isPlaying) 2.dp else 1.dp,
                color = if (isPlaying) AccentGreen else borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (title.contains("Original")) Color(0xFFFF9800) else AccentGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Play/Stop Button - Larger and more visible
        Box(
            modifier = Modifier
                .size(56.dp)
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
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Status text
        Text(
            text = if (isPlaying) "Playing..." else "Tap to play",
            fontSize = 10.sp,
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
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0D1F0D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(
                icon = Icons.Filled.Home,
                label = "Cleaner",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            NavItem(
                icon = Icons.Default.PlayArrow,  // Using PlayArrow as Mic alternative
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
            tint = if (isSelected) Color(0xFF00E676) else Color(0xFF666666),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (isSelected) Color(0xFF00E676) else Color(0xFF666666),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
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
                color = Color(0xFF00E676).copy(alpha = alpha),
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
                    trackColor = Color(0xFF1A2540)
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
fun CleanFileDialog(
    fileName: String,
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Tap 'Clean Audio' to remove noise and enhance your audio file",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
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
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Clean Audio",
                        color = Color.Black,
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
fun ProcessingDialog() {
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
                    "Cleaning Audio...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "AI is removing noise and enhancing your audio. Please wait...",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
