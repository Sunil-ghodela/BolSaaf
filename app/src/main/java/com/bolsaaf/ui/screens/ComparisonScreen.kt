package com.bolsaaf.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ComparisonScreen(
    originalFile: File,
    cleanedFile: File,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var playingOriginal by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(100f) }
    
    var originalPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var cleanedPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(originalFile, cleanedFile) {
        originalPlayer = MediaPlayer().apply {
            setDataSource(originalFile.absolutePath)
            prepare()
        }
        cleanedPlayer = MediaPlayer().apply {
            setDataSource(cleanedFile.absolutePath)
            prepare()
        }
        
        duration = originalPlayer?.duration?.toFloat() ?: 100f
        
        onDispose {
            originalPlayer?.release()
            cleanedPlayer?.release()
        }
    }

    LaunchedEffect(isPlaying, playingOriginal) {
        while (isPlaying) {
            val player = if (playingOriginal) originalPlayer else cleanedPlayer
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
            val player = if (playingOriginal) originalPlayer else cleanedPlayer
            player?.start()
            isPlaying = true
        }
    }

    fun seekTo(position: Float) {
        originalPlayer?.seekTo(position.toInt())
        cleanedPlayer?.seekTo(position.toInt())
        currentPosition = position
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1F0D))
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
                "Compare Audio",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Comparison Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Original Audio Card
            AudioCard(
                modifier = Modifier.weight(1f),
                title = "Original",
                subtitle = "With Noise",
                isSelected = playingOriginal,
                onClick = {
                    playingOriginal = true
                    if (isPlaying) {
                        cleanedPlayer?.pause()
                        originalPlayer?.seekTo(currentPosition.toInt())
                        originalPlayer?.start()
                    }
                },
                color = Color(0xFF666666)
            )

            // Cleaned Audio Card
            AudioCard(
                modifier = Modifier.weight(1f),
                title = "Cleaned",
                subtitle = "AI Enhanced",
                isSelected = !playingOriginal,
                onClick = {
                    playingOriginal = false
                    if (isPlaying) {
                        originalPlayer?.pause()
                        cleanedPlayer?.seekTo(currentPosition.toInt())
                        cleanedPlayer?.start()
                    }
                },
                color = Color(0xFF00E676)
            )
        }

        // Waveform Visualization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A2E1A), Color(0xFF0D1F0D))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (playingOriginal) Color(0xFF666666) else Color(0xFF00E676),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (playingOriginal) "Playing Original" else "Playing Cleaned",
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Fake waveform bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(20) { index ->
                        val height = if (isPlaying) {
                            (20 + kotlin.math.sin((currentPosition / 1000f) + index) * 30).toInt()
                        } else 20
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(height.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (playingOriginal) Color(0xFF666666) else Color(0xFF00E676)
                                )
                        )
                    }
                }
            }
        }

        // Progress Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Slider(
                value = currentPosition,
                onValueChange = ::seekTo,
                valueRange = 0f..duration,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E676),
                    activeTrackColor = Color(0xFF00E676),
                    inactiveTrackColor = Color(0xFF333333)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(currentPosition.toInt()),
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
                Text(
                    formatTime(duration.toInt()),
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Play/Pause Button
        Button(
            onClick = ::togglePlay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E676)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isPlaying) "Pause" else "Play",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Add,
                label = "Save",
                onClick = onSave
            )
            ActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Share,
                label = "Share",
                onClick = onShare
            )
        }
    }
}

@Composable
fun AudioCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF1A2E1A)
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = color,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) color else Color.White
        )
        Text(
            subtitle,
            fontSize = 12.sp,
            color = Color(0xFF888888)
        )
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A2E1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00E676)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

fun formatTime(millis: Int): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
