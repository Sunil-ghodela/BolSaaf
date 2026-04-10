package com.bolsaaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("History", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${audioPairs.size} cleans", fontSize = 13.sp, color = TextSecondary)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(audioPairs, key = { it.timestamp }) { pair ->
                    ComparisonCard(
                        pair = pair,
                        recordingsDir = recordingsDir,
                        currentlyPlaying = currentlyPlaying,
                        onPlayOriginal = {
                            if (currentlyPlaying == pair.originalFile) onStopFile() else onPlayFile(pair.originalFile)
                        },
                        onPlayCleaned = {
                            if (currentlyPlaying == pair.cleanedFile) onStopFile() else onPlayFile(pair.cleanedFile)
                        },
                        onRemove = { onRemovePair(pair.timestamp) },
                        onShare = { onShareFile(pair.cleanedFile) },
                        onDownload = { onDownloadFile(pair.cleanedFile) },
                        onFeedback = { feedbackPair = pair }
                    )
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
