package com.bolsaaf.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HomeScreen(
    isRecording: Boolean = false,
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onUploadFile: () -> Unit = {},
    onGoToHistory: () -> Unit = {},
    recentCleans: List<CleanItem> = emptyList(),
    freeMinutesLeft: Int = 8
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Animated wave effect for recording state
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )
    
    // Pulse effect for main button
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Rotating gradient animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0D2818),
                        Color(0xFF051205)
                    ),
                    center = Offset(0.5f, 0.3f),
                    radius = 0.8f
                )
            )
    ) {
        // Animated background particles
        if (isRecording) {
            AudioWaveformAnimation(modifier = Modifier.fillMaxSize())
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Header with glassmorphism
                GlassmorphicHeader(
                    freeMinutesLeft = freeMinutesLeft,
                    onGoToHistory = onGoToHistory
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Animated title
                AnimatedTitle()
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Main recording button with glow effect
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow rings
                    if (isRecording) {
                        repeat(3) { index ->
                            val delay = index * 300
                            val animatedScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, delayMillis = delay, easing = EaseOutCubic),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "ring_$index"
                            )
                            val alpha = (2f - animatedScale).coerceIn(0f, 0.3f)
                            
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .scale(animatedScale)
                                    .alpha(alpha)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF00E676).copy(alpha = 0.4f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    
                    // Main button with rotating border
                    Box(
                        modifier = Modifier
                            .size(180.dp * pulseScale)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF00E676),
                                        Color(0xFF00C853),
                                        Color(0xFF69F0AE),
                                        Color(0xFF00E676)
                                    ),
                                    center = Offset(0.5f, 0.5f)
                                )
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) 
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFFFF4444), Color(0xFFCC0000))
                                    )
                                else 
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF00E676), Color(0xFF00C853))
                                    )
                            )
                            .clickable { 
                                if (isRecording) onStopRecording() else onStartRecording()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop" else "Record",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isRecording) "Recording..." else "Tap to Clean",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (!isRecording) {
                                Text(
                                    text = "Noise hatao, awaaz saaf karo",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Quick action cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Upload,
                        title = "Upload",
                        subtitle = "File se clean karo",
                        onClick = onUploadFile
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FolderCopy,
                        title = "Batch",
                        subtitle = "Multiple files",
                        badge = "PRO"
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        subtitle = "Customize karo"
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Stats section
                StatsSection()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Recent cleans header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Cleans",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = onGoToHistory) {
                        Text(
                            "See All →",
                            color = Color(0xFF00E676),
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            items(recentCleans.take(3)) { item ->
                CleanedAudioCard(item = item)
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        
        // Bottom navigation bar
        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = 0
        )
    }
}

@Composable
fun GlassmorphicHeader(
    freeMinutesLeft: Int,
    onGoToHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF00E676), Color(0xFF00C853))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "BolSaaf",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "AI Voice Cleaner",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF00E676).copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "$freeMinutesLeft min free",
                        fontSize = 12.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "title")
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Studio jaisi awaaz",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Ghar baithe banao",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF00E676), Color(0xFF69F0AE))
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF142414))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (badge == "PRO") 
                        Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF8F00)))
                    else 
                        Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00C853)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            subtitle,
            fontSize = 11.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = "98%",
            label = "Noise Removed",
            icon = Icons.Default.NoiseAware
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "48000Hz",
            label = "Studio Quality",
            icon = Icons.Default.HighQuality
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "AI",
            label = "RNNoise Engine",
            icon = Icons.Default.AutoFixHigh
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2E1A))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            fontSize = 10.sp,
            color = Color(0xFF888888)
        )
    }
}

@Composable
fun CleanedAudioCard(item: CleanItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF142414)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00E676).copy(alpha = 0.3f),
                                Color(0xFF00C853).copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF00E676).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            item.status,
                            fontSize = 11.sp,
                            color = Color(0xFF00E676),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    item.time,
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF00E676)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    selectedTab: Int = 0
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0D1F0D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(
                icon = Icons.Default.CleaningServices,
                label = "Cleaner",
                isSelected = selectedTab == 0
            )
            NavItem(
                icon = Icons.Default.History,
                label = "History",
                isSelected = selectedTab == 1
            )
            NavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = selectedTab == 2
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
            tint = if (isSelected) Color(0xFF00E676) else Color(0xFF666666),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF00E676) else Color(0xFF666666),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
            )
        }
    }
}

@Composable
fun AudioWaveformAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val time = System.currentTimeMillis() / 1000f
        
        // Draw animated circles
        for (i in 0..3) {
            val radius = 100f + i * 80f + (time * 50f) % 80f
            val alpha = (1f - (radius - 100f) / 320f).coerceIn(0f, 0.3f)
            
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = alpha),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
        }
    }
}

data class CleanItem(
    val name: String,
    val status: String,
    val time: String
)
