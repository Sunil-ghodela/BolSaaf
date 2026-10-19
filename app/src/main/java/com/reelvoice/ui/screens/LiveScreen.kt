package com.reelvoice.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reelvoice.audio.CleaningPreset
import com.reelvoice.ui.components.BottomNavBar
import com.reelvoice.ui.components.BottomNavBarBottomPadding

/** Recording setup first; the last result is secondary and never duplicates the history list. */
@Composable
fun LiveScreen(
    isRecording: Boolean = false,
    selectedTab: Int = 1,
    audioPairs: List<AudioPair> = emptyList(),
    currentlyPlaying: String? = null,
    cleaningPreset: CleaningPreset = CleaningPreset.STRONG,
    showSuccess: Boolean = false,
    onDismissSuccess: () -> Unit = {},
    lastSaveInfo: SaveInfo? = null,
    playbackPositionMs: Int = 0,
    playbackDurationMs: Int = 0,
    isVideoMode: Boolean = false,
    onToggleFormat: (Boolean) -> Unit = {},
    onStartVideoRecording: () -> Unit = {},
    onCleaningPresetChange: (CleaningPreset) -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onPlayFile: (String) -> Unit = {},
    onStopFile: () -> Unit = {},
    onShareFile: (String) -> Unit = {},
    onDownloadFile: (String) -> Unit = {},
    onSubmitFeedback: (AudioPair, Boolean, String?, String?, String?) -> Unit = { _, _, _, _, _ -> }
) {
    var feedbackPair by remember { mutableStateOf<AudioPair?>(null) }
    val preset = cleaningPreset.forRecording()
    // MainActivity supplies a SnapshotStateList: its identity stays the same on save.
    // Read its contents each composition so a newly saved take appears immediately.
    val latestRecording = audioPairs.filter { it.isRecording }.maxByOrNull { it.timestamp }
    // Keep results out of the way while recording. An audio session remains audio even if
    // the caller's format flag changes during a foreground-service reconnection.
    val videoSetup = isVideoMode && !isRecording
    val scrollState = rememberScrollState()
    LaunchedEffect(showSuccess, lastSaveInfo?.cleanedFileName) {
        if (showSuccess) {
            scrollState.animateScrollTo(0)
            kotlinx.coroutines.delay(4000)
            onDismissSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = BottomNavBarBottomPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Record", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "One take. A clearer voice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onTabSelected(2) }) {
                    Icon(Icons.Default.History, contentDescription = "Open recording history")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = !videoSetup,
                            onClick = { onToggleFormat(false) },
                            enabled = !isRecording,
                            label = { Text("Audio") },
                            leadingIcon = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = videoSetup,
                            onClick = { onToggleFormat(true) },
                            enabled = !isRecording,
                            label = { Text("Video") },
                            leadingIcon = { Icon(Icons.Default.Videocam, null, Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (videoSetup) {
                        Text("Record a video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Capture video with original audio. To remove noise afterwards, use Video Clean on Home.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Recording mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            CleaningPreset.recordingPresets.forEach { mode ->
                                FilterChip(
                                    selected = preset == mode,
                                    onClick = { onCleaningPresetChange(mode) },
                                    enabled = !isRecording,
                                    label = { Text(mode.label) },
                                    leadingIcon = {
                                        Icon(
                                            if (preset == mode) Icons.Default.Check else Icons.Default.Mic,
                                            null,
                                            Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            if (preset == CleaningPreset.STRONG) "Strong · more headroom for louder input."
                            else "Studio · a little more input presence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()
                    Text(
                        when {
                            isRecording -> "Recording · ${preset.label}"
                            videoSetup -> "Camera opens on the next step"
                            else -> "Ready to record"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    Button(
                        onClick = {
                            if (isRecording) onStopRecording()
                            else {
                                onStopFile()
                                if (videoSetup) onStartVideoRecording() else onStartRecording()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            when {
                                isRecording -> Icons.Default.Stop
                                videoSetup -> Icons.Default.Videocam
                                else -> Icons.Default.Mic
                            },
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(when {
                            isRecording -> "Stop & save"
                            videoSetup -> "Open video camera"
                            else -> "Start recording"
                        })
                    }
                    Text(
                        when {
                            isRecording -> "Mode is locked during this take. Stop to save your original and cleaned audio."
                            videoSetup -> "Camera and microphone permission are required."
                            else -> "Cleaned on this device. Original and cleaned audio are saved automatically."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isRecording) {
                if (showSuccess && lastSaveInfo != null) {
                    Text(
                        "Recording saved · listen below or open History.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                latestRecording?.let { pair ->
                    LatestRecordingCard(
                        pair = pair,
                        currentlyPlaying = currentlyPlaying,
                        playbackPositionMs = playbackPositionMs,
                        playbackDurationMs = playbackDurationMs,
                        onPlayFile = onPlayFile,
                        onShare = { onShareFile(pair.cleanedFile) },
                        onDownload = { onDownloadFile(pair.cleanedFile) },
                        onFeedback = { feedbackPair = pair }
                    )
                }
                TextButton(onClick = { onTabSelected(2) }, modifier = Modifier.fillMaxWidth()) {
                    Text("View all recordings in History")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
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

@Composable
private fun LatestRecordingCard(
    pair: AudioPair,
    currentlyPlaying: String?,
    playbackPositionMs: Int,
    playbackDurationMs: Int,
    onPlayFile: (String) -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onFeedback: () -> Unit
) {
    // Camera captures are currently raw: don't present the same file as a cleaned result.
    val hasCleanedVersion = pair.originalFile != pair.cleanedFile
    val isPlaying = currentlyPlaying == pair.originalFile || currentlyPlaying == pair.cleanedFile
    OutlinedCard(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Last recording", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(pair.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaybackButton(
                    label = if (hasCleanedVersion) "Cleaned" else "Play recording",
                    isPlaying = currentlyPlaying == pair.cleanedFile,
                    onClick = { onPlayFile(pair.cleanedFile) },
                    modifier = Modifier.weight(1f)
                )
                if (hasCleanedVersion) {
                    PlaybackButton(
                        label = "Original",
                        isPlaying = currentlyPlaying == pair.originalFile,
                        onClick = { onPlayFile(pair.originalFile) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (isPlaying) {
                LinearProgressIndicator(
                    progress = { if (playbackDurationMs > 0) (playbackPositionMs.toFloat() / playbackDurationMs).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${formatPlaybackTime(playbackPositionMs)} / ${formatPlaybackTime(playbackDurationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onShare) { Text("Share") }
                TextButton(onClick = onDownload) { Text("Save copy") }
                if (hasCleanedVersion) {
                    TextButton(onClick = onFeedback) { Text("Feedback") }
                }
            }
        }
    }
}

@Composable
private fun PlaybackButton(label: String, isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (isPlaying) "Stop $label" else label)
    }
}

private fun formatPlaybackTime(ms: Int): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(java.util.Locale.US, seconds / 60, seconds % 60)
}
