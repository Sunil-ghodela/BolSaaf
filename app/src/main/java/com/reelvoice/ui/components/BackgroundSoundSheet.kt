package com.reelvoice.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.reelvoice.audio.AndroidBackgroundPreviewPlayer
import com.reelvoice.audio.BackgroundPreviewPlayer
import com.reelvoice.audio.VoiceBackground
import com.reelvoice.ui.VibeUi

@Composable
fun BackgroundSoundSheet(
    backgrounds: List<VoiceBackground>,
    selectedIndex: Int,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onPicked: (Int) -> Unit,
    previewPlayer: BackgroundPreviewPlayer? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(context, previewPlayer) { previewPlayer ?: AndroidBackgroundPreviewPlayer(context) }
    var choiceId by remember { mutableStateOf(backgrounds.getOrNull(selectedIndex)?.id) }
    var previewId by remember { mutableStateOf<String?>(null) }
    var preparing by remember { mutableStateOf(false) }
    var failedId by remember { mutableStateOf<String?>(null) }
    var request by remember { mutableIntStateOf(0) }

    fun stopPreview() {
        request++
        player.stop()
        previewId = null
        preparing = false
    }
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stopPreview()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); stopPreview() }
    }
    // Refresh/error can remove a playing item without closing the dialog.
    LaunchedEffect(backgrounds, loading, error) {
        if (loading || error != null || backgrounds.none { it.id == previewId }) stopPreview()
    }
    val chosenIndex = backgrounds.indexOfFirst { it.id == choiceId }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Choose a sound", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close sound picker") }
                }
                Text("Listen to a background sample, then choose one for your voice clip.",
                    style = MaterialTheme.typography.bodyMedium)
                Text("Samples contain background only. The final voice + background mix is produced after processing.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        loading -> item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                                Text("Loading sounds…", modifier = Modifier.padding(12.dp))
                            }
                        }
                        error != null -> item {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetry) { Text("Retry sounds") }
                        }
                        backgrounds.isEmpty() -> item {
                            Text("No sounds are available right now.")
                            TextButton(onClick = onRetry) { Text("Refresh sounds") }
                        }
                        else -> items(backgrounds, key = { it.id }) { sound ->
                            val label = VibeUi.displayLabelForBackgroundId(sound.id, sound.label.ifBlank { sound.id })
                            val isChosen = sound.id == choiceId
                            val isPreview = previewId == sound.id
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (isChosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Row(
                                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                                                .semantics {}
                                                .clickable(role = Role.RadioButton) { choiceId = sound.id },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = isChosen, onClick = null)
                                            Text(label, style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(start = 8.dp, end = 8.dp))
                                        }
                                        if (!sound.previewUrl.isNullOrBlank()) {
                                            IconButton(onClick = {
                                                if (isPreview) stopPreview() else {
                                                    stopPreview()
                                                    failedId = null
                                                    previewId = sound.id
                                                    preparing = true
                                                    val token = request
                                                    player.play(sound.previewUrl,
                                                        onPlaying = { if (token == request) preparing = false },
                                                        onComplete = { if (token == request) { previewId = null; preparing = false } },
                                                        onError = { if (token == request) { failedId = sound.id; previewId = null; preparing = false } })
                                                }
                                            }) {
                                                Icon(if (isPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = when {
                                                        isPreview -> "Stop preview: ${sound.label}"
                                                        failedId == sound.id -> "Retry preview: ${sound.label}"
                                                        else -> "Preview: ${sound.label}"
                                                    })
                                            }
                                        }
                                    }
                                    if (isPreview) Text(if (preparing) "Loading sample…" else "Playing background sample",
                                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                                    if (failedId == sound.id) Text("Sample couldn't play. Tap retry preview, or choose another sound.",
                                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp))
                                    if (sound.previewUrl.isNullOrBlank()) Text("Sample unavailable",
                                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { stopPreview(); onPicked(chosenIndex) },
                    enabled = chosenIndex >= 0 && !loading && error == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Use sound & pick voice clip") }
            }
        }
    }
}
