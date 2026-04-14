package com.bolsaaf.ui.screens

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import com.bolsaaf.ui.theme.AccentCyan
import com.bolsaaf.ui.theme.AccentGreen
import com.bolsaaf.ui.theme.AccentPurple
import com.bolsaaf.ui.theme.BackgroundCard
import com.bolsaaf.ui.theme.BackgroundDark
import com.bolsaaf.ui.theme.TextPrimary
import com.bolsaaf.ui.theme.TextSecondary

@Composable
fun ComparisonPlayerScreen(
    originalFile: File,
    cleanedFile: File,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isPlayingCleaned by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(100f) }
    var showSuccess by remember { mutableStateOf(false) }
    
    var originalPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var cleanedPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Success animation
    LaunchedEffect(Unit) {
        showSuccess = true
        delay(2000)
        showSuccess = false
    }

    DisposableEffect(originalFile, cleanedFile) {
        originalPlayer = MediaPlayer().apply {
            try {
                setDataSource(originalFile.absolutePath)
                prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        cleanedPlayer = MediaPlayer().apply {
            try {
                setDataSource(cleanedFile.absolutePath)
                prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        duration = cleanedPlayer?.duration?.toFloat() ?: 100f
        
        onDispose {
            originalPlayer?.release()
            cleanedPlayer?.release()
        }
    }

    LaunchedEffect(isPlaying, isPlayingCleaned) {
        while (isPlaying) {
            val player = if (isPlayingCleaned) cleanedPlayer else originalPlayer
            currentPosition = player?.currentPosition?.toFloat() ?: 0f
            delay(100)
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            originalPlayer?.pause()
            cleanedPlayer?.pause()
            isPlaying = false
        } else {
            val player = if (isPlayingCleaned) cleanedPlayer else originalPlayer
            player?.start()
            isPlaying = true
        }
    }

    fun switchVersion(playCleaned: Boolean) {
        if (isPlayingCleaned == playCleaned) return
        
        val currentPos = currentPosition.toInt()
        
        if (isPlaying) {
            if (playCleaned) {
                originalPlayer?.pause()
                cleanedPlayer?.seekTo(currentPos)
                cleanedPlayer?.start()
            } else {
                cleanedPlayer?.pause()
                originalPlayer?.seekTo(currentPos)
                originalPlayer?.start()
            }
        }
        isPlayingCleaned = playCleaned
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentPurple.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(0.5f, 0f),
                        radius = 0.6f
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Before vs After",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Success message animation
            AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = AccentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "✨ Voice Enhanced!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Toggle Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BackgroundCard),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Before Button
                VersionToggleButton(
                    isSelected = !isPlayingCleaned,
                    onClick = { switchVersion(false) },
                    label = "Before",
                    subtitle = "Original",
                    color = TextSecondary
                )

                // After Button
                VersionToggleButton(
                    isSelected = isPlayingCleaned,
                    onClick = { switchVersion(true) },
                    label = "After",
                    subtitle = "Enhanced",
                    color = AccentGreen
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Audio Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BackgroundCard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Animated waveform bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(100.dp)
                    ) {
                        val barCount = 20
                        repeat(barCount) { index ->
                            val animatedHeight by animateFloatAsState(
                                targetValue = if (isPlaying) {
                                    (30 + kotlin.math.sin((currentPosition / 500f) + index * 0.3f) * 40).coerceIn(10f, 80f)
                                } else 20f,
                                animationSpec = tween(100),
                                label = "bar_$index"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(animatedHeight.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isPlayingCleaned) 
                                            Brush.verticalGradient(
                                                colors = listOf(AccentGreen, AccentCyan)
                                            )
                                        else 
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Gray, Color.DarkGray)
                                            )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        if (isPlayingCleaned) "✨ Clean Audio" else "🔊 Original Audio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isPlayingCleaned) AccentGreen else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                LinearProgressIndicator(
                    progress = { currentPosition / duration },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isPlayingCleaned) AccentGreen else Color.Gray,
                    trackColor = BackgroundCard
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentPosition.toInt()),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        formatTime(duration.toInt()),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Play Button
            Button(
                onClick = ::togglePlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlayingCleaned) AccentGreen else TextSecondary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isPlaying) "Pause" else "Play",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge(icon = "🔇", label = "98%", sublabel = "Noise Removed")
                StatBadge(icon = "🎵", label = "48kHz", sublabel = "Studio Quality")
                StatBadge(icon = "⚡", label = "AI", sublabel = "Enhanced")
            }
        }
    }
}

@Composable
fun VersionToggleButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String,
    subtitle: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) color else BackgroundCard
                )
                .border(
                    width = if (isSelected) 0.dp else 2.dp,
                    color = color.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (label == "Before") Icons.Filled.Close else Icons.Filled.Check,
                contentDescription = null,
                tint = if (isSelected) Color.Black else color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) color else Color.White
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun StatBadge(icon: String, label: String, sublabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            sublabel,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
