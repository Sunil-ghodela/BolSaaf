package com.bolsaaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter { All, Audio, Video, Completed }

@Composable
fun HistoryScreen(
    recordingsDir: File,
    selectedTab: Int = 2,
    audioPairs: List<AudioPair> = emptyList(),
    currentlyPlaying: String? = null,
    onTabSelected: (Int) -> Unit = {},
    onPlayFile: (String) -> Unit = {},
    onStopFile: () -> Unit = {},
    onRemovePair: (String) -> Unit = {},
    onShareFile: (String) -> Unit = {},
    onDownloadFile: (String) -> Unit = {},
    onSubmitFeedback: (AudioPair, Boolean, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onBack: () -> Unit = {}
) {
    var feedbackPair by remember { mutableStateOf<AudioPair?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HistoryFilter.All) }

    val filtered = remember(audioPairs, query, filter) {
        audioPairs.filter { pair ->
            val q = query.trim().lowercase(Locale.getDefault())
            val hay = "${pair.originalFile} ${pair.cleanedFile}".lowercase(Locale.getDefault())
            val matchesQuery = q.isEmpty() || hay.contains(q)
            val cleaned = File(recordingsDir, pair.cleanedFile)
            val isVideo = isVideoFile(pair.cleanedFile)
            val isAudio = isAudioFile(pair.cleanedFile)
            val exists = cleaned.exists()
            val matchesType = when (filter) {
                HistoryFilter.All -> true
                HistoryFilter.Audio -> isAudio
                HistoryFilter.Video -> isVideo
                HistoryFilter.Completed -> exists
            }
            matchesQuery && matchesType
        }
    }

    val sections = remember(filtered) { groupHistoryBySection(filtered) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BackgroundDark, Color(0xFF0D1420), BackgroundDark)
                    )
                )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("History", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Audio & video files", fontSize = 14.sp, color = TextSecondary)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text("Search audio or video…", color = TextSecondary.copy(alpha = 0.7f))
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentGreen.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    cursorColor = AccentGreen,
                    focusedContainerColor = Color(0xFF121A2A),
                    unfocusedContainerColor = Color(0xFF121A2A)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterChip("All", filter == HistoryFilter.All) { filter = HistoryFilter.All }
                HistoryFilterChip("Audio", filter == HistoryFilter.Audio) { filter = HistoryFilter.Audio }
                HistoryFilterChip("Video", filter == HistoryFilter.Video) { filter = HistoryFilter.Video }
                HistoryFilterChip("Completed", filter == HistoryFilter.Completed) {
                    filter = HistoryFilter.Completed
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "No files match filters.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
                        )
                    }
                } else {
                    sections.forEach { (title, pairs) ->
                        item {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(pairs, key = { it.timestamp }) { pair ->
                            HistoryMediaRow(
                                pair = pair,
                                recordingsDir = recordingsDir,
                                currentlyPlaying = currentlyPlaying,
                                onPlay = {
                                    if (currentlyPlaying == pair.cleanedFile) onStopFile()
                                    else onPlayFile(pair.cleanedFile)
                                },
                                onSave = { onDownloadFile(pair.cleanedFile) },
                                onShare = { onShareFile(pair.cleanedFile) },
                                onFeedback = { feedbackPair = pair },
                                onDelete = { onRemovePair(pair.timestamp) }
                            )
                        }
                    }
                }
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

        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun HistoryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AccentGreen,
            selectedLabelColor = Color.Black,
            containerColor = Color(0xFF1A2438),
            labelColor = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.12f)
        )
    )
}

private fun groupHistoryBySection(pairs: List<AudioPair>): List<Pair<String, List<AudioPair>>> {
    if (pairs.isEmpty()) return emptyList()
    val todayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    val byDay = pairs.groupBy { pair ->
        pair.timestamp.take(8).ifBlank { todayKey }
    }.mapValues { (_, list) -> list.sortedByDescending { it.timestamp } }
    val orderedDays = byDay.keys.sortedDescending()
    return orderedDays.map { day ->
        val title = if (day == todayKey) "TODAY" else formatSectionDate(day)
        title to (byDay[day] ?: emptyList())
    }
}

private fun formatSectionDate(ymd: String): String {
    return try {
        val d = SimpleDateFormat("yyyyMMdd", Locale.US).parse(ymd) ?: return ymd
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(d)
    } catch (_: Exception) {
        ymd
    }
}

private fun isVideoFile(name: String): Boolean {
    val n = name.lowercase(Locale.getDefault())
    return n.endsWith(".mp4") || n.endsWith(".mov") || n.endsWith(".webm") || n.endsWith(".mkv")
}

private fun isAudioFile(name: String): Boolean {
    val n = name.lowercase(Locale.getDefault())
    return n.endsWith(".wav") || n.endsWith(".mp3") || n.endsWith(".aac") || n.endsWith(".m4a") || n.endsWith(".flac")
}

@Composable
private fun HistoryMediaRow(
    pair: AudioPair,
    recordingsDir: File,
    currentlyPlaying: String?,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onFeedback: () -> Unit,
    onDelete: () -> Unit
) {
    val cleaned = File(recordingsDir, pair.cleanedFile)
    val sizeBytes = if (cleaned.exists()) cleaned.length() else 0L
    val video = isVideoFile(pair.cleanedFile)
    val playing = currentlyPlaying == pair.cleanedFile
    val done = cleaned.exists()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A2A)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = if (video) {
                            Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFF880E4F)))
                        } else {
                            Brush.linearGradient(listOf(AccentPurple, AccentGreen))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (video) Icons.Filled.Star else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pair.cleanedFile,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (done) AccentGreen else Color(0xFFFFCA28))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val type = if (video) "Video" else "Audio"
                val dur = if (pair.durationSec > 0f) {
                    formatDuration(pair.durationSec)
                } else {
                    "—"
                }
                Text(
                    text = "${formatFileSize(sizeBytes)} · $type · $dur",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (playing) AccentGreen.copy(alpha = 0.25f) else Color(0xFF1E2A40),
                    modifier = Modifier.clickable { onPlay() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = if (playing) AccentGreen else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (playing) "Stop" else "Play",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (playing) AccentGreen else Color.White
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2A40),
                    modifier = Modifier.clickable { onSave() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Save",
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pair.time,
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Share", tint = TextSecondary)
            }
            IconButton(onClick = onFeedback, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Info, contentDescription = "Feedback", tint = TextSecondary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color(0xFFEF5350))
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(Locale.US, kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(Locale.US, mb)
}

private fun formatDuration(sec: Float): String {
    val s = sec.toInt().coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "%d:%02d".format(Locale.US, m, r)
}
