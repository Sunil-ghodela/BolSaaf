package com.bolsaaf

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bolsaaf.audio.RNNoise
import com.bolsaaf.audio.AudioProcessor
import com.bolsaaf.audio.AudioRecorder
import com.bolsaaf.ui.theme.BolSaafTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var audioProcessor: AudioProcessor
    private lateinit var audioRecorder: AudioRecorder
    private var tempRecordingFile: File? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permissions granted
        }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processAudioFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioProcessor = AudioProcessor(this)
        audioRecorder = AudioRecorder(this)

        checkPermissions()

        setContent {
            BolSaafTheme {
                MainScreen(
                    onStartCleaning = { startCleaning() },
                    onUploadFile = { pickAudioLauncher.launch("audio/*") },
                    onBatchProcess = { /* TODO */ },
                    onGoPro = { /* TODO */ },
                    recentCleans = getRecentCleans()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioProcessor.destroy()
        audioRecorder.stopRecording()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val needPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(needPermissions.toTypedArray())
        }
    }

    private fun startCleaning() {
        if (audioRecorder.isRecording()) {
            audioRecorder.stopRecording()
        } else {
            // Initialize RNNoise
            if (!audioProcessor.initialize()) {
                // Handle initialization error
                return
            }
            
            tempRecordingFile = File(cacheDir, "temp_recording_${System.currentTimeMillis()}.wav")
            
            // Set up real-time noise cancellation during recording
            audioRecorder.setCallbacks(
                onData = { buffer ->
                    // Process each 480-sample frame through RNNoise
                    audioProcessor.processFrame(buffer)
                },
                onFinished = { file ->
                    // Clean up
                    audioProcessor.destroy()
                }
            )
            
            audioRecorder.startRecording(tempRecordingFile!!)
        }
    }

    private fun processAudioFile(uri: Uri) {
        // Initialize RNNoise
        if (!audioProcessor.initialize()) {
            return
        }
        
        val outputFile = File(cacheDir, "cleaned_${System.currentTimeMillis()}.wav")
        
        // Process in background
        Thread {
            val success = audioProcessor.cleanAudioFile(uri, outputFile) { progress ->
                runOnUiThread {
                    // Update progress UI if needed
                }
            }
            
            runOnUiThread {
                audioProcessor.destroy()
                if (success) {
                    // Handle success - outputFile contains cleaned audio
                }
            }
        }.start()
    }

    private fun getRecentCleans(): List<CleanItem> {
        // TODO: Load from database or preferences
        return listOf(
            CleanItem("Lecture_2026.mp3", "98% noise removed", "2h ago"),
            CleanItem("Reel_Traffic.mp4", "Cleaned just now", "5m ago")
        )
    }
}

data class CleanItem(val name: String, val status: String, val time: String)

@Composable
fun MainScreen(
    onStartCleaning: () -> Unit,
    onUploadFile: () -> Unit,
    onBatchProcess: () -> Unit,
    onGoPro: () -> Unit,
    recentCleans: List<CleanItem>
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1A0A),
                        Color(0xFF051205)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "BolSaaf",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Studio jaisa sound,\nghar baithhe",
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.End,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Profile icon placeholder
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A3A2A))
                        ) {
                            Text(
                                "S",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Main title
                Text(
                    "Apni awaaz ko saaf karo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Text(
                    "Noise hatao, voice chamkao – 1 tap mein!",
                    fontSize = 16.sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Main CTA Button
                Box(
                    modifier = Modifier
                        .size(180.dp * scale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676),
                                    Color(0xFF00C853)
                                )
                            )
                        )
                        .clickable { onStartCleaning() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start Cleaning",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap to clean voice",
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Upload,
                        title = "File Upload",
                        subtitle = "Audio/Video upload\nkarke saaf karo",
                        buttonText = "Choose File",
                        onClick = onUploadFile
                    )
                    
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FolderCopy,
                        title = "Batch Process",
                        subtitle = "Multiple files ek saath",
                        badge = "PRO",
                        onClick = onBatchProcess
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Recent Cleans
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Cleans",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "See all →",
                        fontSize = 14.sp,
                        color = Color(0xFF00E676)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            items(recentCleans) { item ->
                RecentCleanItem(item)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                // How it Works
                Text(
                    "How it Works",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Mic,
                        title = "Record\nor Upload",
                        subtitle = "Noise detected"
                    )
                    StepCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoFixHigh,
                        title = "AI Magic",
                        subtitle = "DeepFilterNet in action"
                    )
                    StepCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Download,
                        title = "Download",
                        subtitle = "Clean Audio"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Free: 8 min left today • Go Pro",
                        fontSize = 14.sp,
                        color = Color(0xFFAAAAAA)
                    )
                    
                    Row {
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1A2A1A), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color(0xFF00E676)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1A2A1A), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFF00E676)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Bottom Navigation
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = Color(0xFF0D1F0D)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    icon = Icons.Default.Waves,
                    label = "Cleaner",
                    isSelected = true
                )
                NavItem(
                    icon = Icons.Default.History,
                    label = "History"
                )
                NavItem(
                    icon = Icons.Default.Settings,
                    label = "Settings"
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String? = null,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF142414)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (badge != null) {
                    Surface(
                        color = Color(0xFFFFB300),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color(0xFF888888),
                lineHeight = 16.sp
            )
            
            if (buttonText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun RecentCleanItem(item: CleanItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF142414)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00E676).copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    item.status,
                    fontSize = 12.sp,
                    color = Color(0xFF00E676)
                )
            }
            
            Text(
                item.time,
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
        }
    }
}

@Composable
fun StepCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF142414)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            
            Text(
                subtitle,
                fontSize = 10.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF00E676) else Color(0xFF888888),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF00E676) else Color(0xFF888888)
        )
    }
}
