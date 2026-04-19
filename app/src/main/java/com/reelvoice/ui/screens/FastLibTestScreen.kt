package com.reelvoice.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelvoice.ui.components.BottomNavBar
import com.reelvoice.ui.theme.AccentCyan
import com.reelvoice.ui.theme.AccentGreen
import com.reelvoice.ui.theme.BackgroundCard
import com.reelvoice.ui.theme.CtaOrangeRedGradient
import com.reelvoice.ui.theme.SliderTrackStrong
import com.reelvoice.ui.theme.TextPrimary
import com.reelvoice.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FastLibTestScreen(
    selectedTab: Int = 4,
    recordingsDir: File,
    selectedFileLabel: String? = null,
    isVideoInput: Boolean = false,
    isCleaning: Boolean = false,
    stage: String? = null,
    progressPct: Int = 0,
    outputs: List<String> = emptyList(),
    maxAudioMb: Int = 25,
    maxVideoMb: Int = 50,
    onUploadAudio: () -> Unit = {},
    onUploadVideo: () -> Unit = {},
    onStartCleaning: () -> Unit = {},
    onPlayOutput: (String) -> Unit = {},
    onShareOutput: (String) -> Unit = {},
    onSaveOutput: (String) -> Unit = {},
    onTabSelected: (Int) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 96.dp)
        ) {
            item {
                Text(
                    text = "Fast Lib Voice Clean Test",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Upload audio/video and run studio voice cleaning test flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                LimitsInfoCard(
                    maxAudioMb = maxAudioMb,
                    maxVideoMb = maxVideoMb
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UploadBox(
                        modifier = Modifier.weight(1f),
                        title = "Upload Audio",
                        subtitle = "MP3, WAV, AAC · ≤${maxAudioMb} MB",
                        onClick = onUploadAudio
                    )
                    UploadBox(
                        modifier = Modifier.weight(1f),
                        title = "Upload Video",
                        subtitle = "MP4, MOV, AVI · ≤${maxVideoMb} MB",
                        onClick = onUploadVideo
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Selected Input", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedFileLabel ?: "No file selected",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedFileLabel != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isVideoInput) "Input type: Video -> clean + remux" else "Input type: Audio -> clean voice",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onStartCleaning,
                    enabled = selectedFileLabel != null && !isCleaning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clean Voice (Fast Lib Test)")
                }

                if (isCleaning) {
                    Spacer(modifier = Modifier.height(14.dp))
                    PhaseProgressCard(stage = stage, progressPct = progressPct)
                }

                if (outputs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Outputs (${outputs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(outputs, key = { it }) { name ->
                val file = File(recordingsDir, name)
                OutputRow(
                    file = file,
                    onPlay = { onPlayOutput(name) },
                    onShare = { onShareOutput(name) },
                    onSave = { onSaveOutput(name) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun LimitsInfoCard(
    maxAudioMb: Int,
    maxVideoMb: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentCyan.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = "LAB LIMITS",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "fast-music-remover",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LimitLine(
                label = "Audio",
                detail = "≤ $maxAudioMb MB · MP3, WAV, AAC, M4A, FLAC, OGG"
            )
            Spacer(modifier = Modifier.height(4.dp))
            LimitLine(
                label = "Video",
                detail = "≤ $maxVideoMb MB · MP4, MOV, MKV, WebM, AVI"
            )
            Spacer(modifier = Modifier.height(4.dp))
            LimitLine(
                label = "Engine",
                detail = "DeepFilterNet3 · CPU only · ~1s per 5-10s of audio"
            )
        }
    }
}

@Composable
private fun LimitLine(label: String, detail: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.width(54.dp),
            fontSize = 11.sp
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PhaseProgressCard(stage: String?, progressPct: Int) {
    val (title, subtitle, accent) = when (stage) {
        "uploading" -> Triple("Uploading…", "Sending your file to the server", AccentCyan)
        "processing" -> Triple("Cleaning Audio…", "FastLib is isolating the voice track", AccentGreen)
        "downloading" -> Triple(
            if (progressPct > 0) "Downloading $progressPct%" else "Downloading…",
            "Saving cleaned file to your phone",
            AccentCyan
        )
        else -> Triple("Queued…", "Waiting for worker", TextSecondary)
    }
    val frac = if (stage == "downloading") progressPct / 100f else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (frac != null) {
                    CircularProgressIndicator(
                        progress = frac,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 3.dp,
                        color = accent,
                        trackColor = accent.copy(alpha = 0.22f)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 3.dp,
                        color = accent,
                        trackColor = accent.copy(alpha = 0.22f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Step dots (uploading → processing → downloading)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StepDot(label = "Upload", isActive = stage == "uploading", isDone = stage == "processing" || stage == "downloading", accent = accent)
                StepDot(label = "Clean", isActive = stage == "processing", isDone = stage == "downloading", accent = accent)
                StepDot(label = "Download", isActive = stage == "downloading", isDone = false, accent = accent, progressPct = if (stage == "downloading") progressPct else 0)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StepDot(
    label: String,
    isActive: Boolean,
    isDone: Boolean,
    accent: Color,
    progressPct: Int = 0
) {
    val color = when {
        isDone -> accent
        isActive -> accent
        else -> TextSecondary.copy(alpha = 0.35f)
    }
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = when {
                isDone -> 1f
                isActive && progressPct > 0 -> progressPct / 100f
                isActive -> 0.6f
                else -> 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = TextSecondary.copy(alpha = 0.12f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive || isDone) TextPrimary else TextSecondary,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun OutputRow(
    file: File,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    val name = file.name
    val isVideo = name.endsWith(".mp4", true) || name.endsWith(".mov", true) || name.endsWith(".mkv", true)
    val accent = if (isVideo) AccentCyan else AccentGreen

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 84.dp, height = 60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        VideoThumbnail(file = file, contentSize = 60.dp)
                    }
                    // Always overlay a play icon
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Filled.PlayArrow else Icons.Filled.MusicNote,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.18f),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = if (isVideo) "VIDEO" else "AUDIO",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = humanSize(file.length()),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(if (isVideo) "Open" else "Play", fontSize = 12.sp)
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onShare) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Share", fontSize = 12.sp)
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onSave) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Save", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(file: File, contentSize: androidx.compose.ui.unit.Dp) {
    var bmp by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file.absolutePath) {
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                val r = MediaMetadataRetriever()
                r.setDataSource(file.absolutePath)
                val frame = r.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                r.release()
                frame
            }.getOrNull()
        }
    }
    bmp?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Video thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    else -> "%.2f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
}

@Composable
private fun UploadBox(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .border(1.dp, SliderTrackStrong.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = BackgroundCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(brush = CtaOrangeRedGradient, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("↑", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
