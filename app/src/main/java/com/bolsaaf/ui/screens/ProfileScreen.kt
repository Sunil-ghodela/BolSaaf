package com.bolsaaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolsaaf.audio.CleaningPreset

@Composable
fun ProfileScreen(
    selectedTab: Int = 3,
    onTabSelected: (Int) -> Unit = {},
    cleaningPreset: CleaningPreset = CleaningPreset.NORMAL,
    freeMinutesLeft: Int = 2,
    completedCleans: Int = 0,
    onUpgrade: () -> Unit = {},
    onShareRecord: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(220.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AccentPurple, AccentGreen, AccentCyan)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Studio profile",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Track your cleans, presets, and boosts",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${completedCleans} sessions",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Free ${freeMinutesLeft} min left",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = cleaningPreset.label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Completed cleans",
                    value = "$completedCleans",
                    description = "Studio-ready",
                    icon = Icons.Filled.Star
                )
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Preset locked",
                    value = cleaningPreset.label,
                    description = "Auto-adjusted",
                    icon = Icons.Filled.Lock
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            ProfilePlanCard(onUpgrade = onUpgrade)

            Spacer(modifier = Modifier.height(18.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Insights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                ProfileInsightRow(
                    icon = Icons.Filled.CheckCircle,
                    title = "Adaptive preset live",
                    subtitle = "Your voice is now mapped to ${cleaningPreset.label} automatically"
                )
                Divider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )
                ProfileInsightRow(
                    icon = Icons.Filled.Info,
                    title = "Cloud sync",
                    subtitle = "Last backup 12 hours ago · share results anytime"
                )
                Divider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )
                ProfileInsightRow(
                    icon = Icons.Filled.Share,
                    title = "Shareable beats",
                    subtitle = "Tap to share your cleaned tracks in one tap"
                )
                Spacer(modifier = Modifier.height(60.dp))
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
fun ProfileStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF182131)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = AccentCyan)
                }
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ProfilePlanCard(onUpgrade: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF060F19)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Level up to Pro",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock long sessions, priority reels, and background mixes.",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Upgrade & unlock more", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileInsightRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentGreen)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
