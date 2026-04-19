package com.reelvoice.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    LocalContext.current
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

    val originalAccent = MaterialTheme.colorScheme.onSurfaceVariant
    val cleanedAccent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Compare Audio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                accent = originalAccent
            )

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
                accent = cleanedAccent
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surface,
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (playingOriginal) originalAccent else cleanedAccent,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (playingOriginal) "Playing Original" else "Playing Cleaned",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                                .background(if (playingOriginal) originalAccent else cleanedAccent)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    thumbColor = cleanedAccent,
                    activeTrackColor = cleanedAccent,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(currentPosition.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatTime(duration.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = ::togglePlay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isPlaying) "Pause" else "Play",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
    accent: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) accent.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = accent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

fun formatTime(millis: Int): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
