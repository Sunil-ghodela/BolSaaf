package com.bolsaaf.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolsaaf.ui.components.BottomNavBar
import com.bolsaaf.ui.theme.BackgroundCard
import com.bolsaaf.ui.theme.CtaOrangeRedGradient
import com.bolsaaf.ui.theme.SliderTrackStrong
import com.bolsaaf.ui.theme.TextPrimary
import com.bolsaaf.ui.theme.TextSecondary

@Composable
fun FastLibTestScreen(
    selectedTab: Int = 4,
    selectedFileLabel: String? = null,
    isVideoInput: Boolean = false,
    isCleaning: Boolean = false,
    outputFileName: String? = null,
    onUploadAudio: () -> Unit = {},
    onUploadVideo: () -> Unit = {},
    onStartCleaning: () -> Unit = {},
    onPlayOutput: () -> Unit = {},
    onShareOutput: () -> Unit = {},
    onSaveOutput: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
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
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UploadBox(
                    modifier = Modifier.weight(1f),
                    title = "Upload Audio",
                    subtitle = "MP3, WAV, AAC",
                    onClick = onUploadAudio
                )
                UploadBox(
                    modifier = Modifier.weight(1f),
                    title = "Upload Video",
                    subtitle = "MP4, MOV, AVI",
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
                    Text(
                        text = "Selected Input",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
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

            if (outputFileName != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Output Ready: $outputFileName",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onPlayOutput) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Play")
                    }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onShareOutput) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Share")
                    }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onSaveOutput) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Save")
                    }
                }
            }
        }

        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        if (isCleaning) {
            ProcessingDialog(
                title = "Fast Lib cleaning...",
                message = "Voice cleanup is running for your selected media."
            )
        }
    }
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
