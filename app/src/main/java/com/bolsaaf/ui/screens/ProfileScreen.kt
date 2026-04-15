package com.bolsaaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bolsaaf.audio.CleaningPreset
import com.bolsaaf.ui.animation.MD3Motion
import com.bolsaaf.ui.components.BottomNavBar
import com.bolsaaf.ui.animation.slideInFromBottom
import com.bolsaaf.ui.animation.slideInFromTop
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    selectedTab: Int = 3,
    onTabSelected: (Int) -> Unit = {},
    cleaningPreset: CleaningPreset = CleaningPreset.NORMAL,
    freeMinutesLeft: Int = 8,
    freeQuotaMinutes: Int = 8,
    completedCleans: Int = 0,
    totalProcessedMinutes: Float = 0f,
    dayStreak: Int = 0,
    displayName: String = "Creator",
    userHandle: String = "@bolsaaf",
    userEmail: String? = null,
    isLoggedIn: Boolean = false,
    showProMemberBadge: Boolean = false,
    onUpgrade: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onLogin: (String, String) -> Unit = { _, _ -> },
    onRegister: (String, String, String?) -> Unit = { _, _, _ -> },
    onPasswordReset: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val scroll = rememberScrollState()
    var showLoginDialog by remember { mutableStateOf(false) }
    var showPlanDialog by remember { mutableStateOf(false) }

    val renewalLabel = remember {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, 1)
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(c.time)
    }
    val usedFree = (freeQuotaMinutes - freeMinutesLeft).coerceIn(0, freeQuotaMinutes)
    val usedFraction =
        if (freeQuotaMinutes <= 0) 0f else usedFree / freeQuotaMinutes.toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(bottom = 100.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = true,
                enter = slideInFromTop() + fadeIn(animationSpec = MD3Motion.EmphasizedTween)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Identity card
            AnimatedVisibility(
                visible = true,
                enter = slideInFromBottom() + fadeIn(animationSpec = MD3Motion.StandardTween),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(10.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                    ),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "B",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = userHandle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xFFFFB74D), Color(0xFFFF7043))
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (showProMemberBadge) "Pro Member" else "Free plan",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatBlock(
                            value = completedCleans.toString(),
                            label = "Files cleaned",
                            valueColor = MaterialTheme.colorScheme.secondary
                        )
                        ProfileStatBlock(
                            value = formatAudioHours(totalProcessedMinutes),
                            label = "Audio processed",
                            valueColor = MaterialTheme.colorScheme.tertiary
                        )
                        ProfileStatBlock(
                            value = dayStreak.toString(),
                            label = "Day streak",
                            valueColor = Color(0xFFFF80AB)
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plan / quota card
            AnimatedVisibility(
                visible = true,
                enter = slideInFromBottom() + fadeIn(animationSpec = MD3Motion.StandardTween),
                modifier = Modifier.fillMaxWidth()
            ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showProMemberBadge) "Pro Plan" else "Free plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33FF9800)
                        ) {
                            Text(
                                text = "Active",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Renews on $renewalLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Free minutes left", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$freeMinutesLeft / $freeQuotaMinutes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(usedFraction.coerceIn(0f, 1f).coerceAtLeast(0.04f))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primary)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showPlanDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFFFF9800), Color(0xFFFFCA28))
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "View plans",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(18.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInFromBottom() + fadeIn(animationSpec = MD3Motion.ExpressiveTween),
                modifier = Modifier.fillMaxWidth()
            ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "💎 Real insight",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reel mode bundles clean + vibe + loudness in one flow. Preset: ${cleaningPreset.label} — tune from Home when you want a calmer or stronger pass.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
            }

            // Login/Logout Section
            AnimatedVisibility(
                visible = true,
                enter = slideInFromBottom() + fadeIn(animationSpec = MD3Motion.StandardTween),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        if (isLoggedIn && userEmail != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Logged in as",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onLogout,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Logout")
                            }
                        } else {
                            Text(
                                text = "Sign in to sync your data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showLoginDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Login with Email")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login / Register Dialog
            if (showLoginDialog) {
                LoginDialog(
                    onDismiss = { showLoginDialog = false },
                    onLogin = { email, password ->
                        onLogin(email, password)
                        showLoginDialog = false
                    },
                    onRegister = { email, password, name ->
                        onRegister(email, password, name)
                        showLoginDialog = false
                    },
                    onPasswordReset = { email -> onPasswordReset(email) }
                )
            }
        }

        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        if (showPlanDialog) {
            PlanDialog(
                freeMinutesLeft = freeMinutesLeft,
                freeQuotaMinutes = freeQuotaMinutes,
                isProMember = showProMemberBadge,
                renewalLabel = renewalLabel,
                onDismiss = { showPlanDialog = false },
                onUpgrade = {
                    showPlanDialog = false
                    onUpgrade()
                }
            )
        }
    }
}

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String?) -> Unit,
    onPasswordReset: (String) -> Unit
) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    val canSubmit = email.isNotBlank() && password.length >= 6 &&
        (!isRegister || displayName.isNotBlank())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (isRegister) "Create your account" else "Sign in",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRegister) "Email + password — that's it."
                           else "Sign in with your BolSaaf account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (isRegister) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isRegister) "Password (min 6 chars)" else "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isRegister) {
                    TextButton(
                        onClick = { onPasswordReset(email) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isRegister) onRegister(email, password, displayName.ifBlank { null })
                            else onLogin(email, password)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit
                    ) {
                        Text(if (isRegister) "Sign up" else "Sign in")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { isRegister = !isRegister },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isRegister) "Already have an account? Sign in"
                               else "New to BolSaaf? Create an account",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatBlock(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun formatAudioHours(minutes: Float): String {
    if (minutes <= 0f) return "0m"
    val h = (minutes / 60f).toInt()
    val m = (minutes % 60f).toInt().coerceAtLeast(0)
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun PlanDialog(
    freeMinutesLeft: Int,
    freeQuotaMinutes: Int,
    isProMember: Boolean,
    renewalLabel: String,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    val usedFraction = if (freeQuotaMinutes <= 0) 0f
        else ((freeQuotaMinutes - freeMinutesLeft).coerceAtLeast(0) / freeQuotaMinutes.toFloat())
            .coerceIn(0f, 1f)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your plan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Current usage progress
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "This month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$freeMinutesLeft / $freeQuotaMinutes min left",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = usedFraction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Resets on $renewalLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Free plan card
                PlanCard(
                    title = "Free",
                    price = "₹0",
                    priceSuffix = "/month",
                    isCurrent = !isProMember,
                    accentColor = MaterialTheme.colorScheme.primary,
                    bullets = listOf(
                        "$freeQuotaMinutes min voice cleaning / month",
                        "Basic + standard cleaning modes",
                        "Cloud + on-device processing",
                        "Video audio clean (up to 50 MB)"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pro plan card
                PlanCard(
                    title = "Pro",
                    price = "₹299",
                    priceSuffix = "/month",
                    isCurrent = isProMember,
                    accentColor = Color(0xFFFF9800),
                    isHighlighted = true,
                    bullets = listOf(
                        "Unlimited voice cleaning",
                        "Studio + Pro cleaning modes (DeepFilterNet)",
                        "FastLib lab access",
                        "Priority processing queue",
                        "Video export without watermark",
                        "Reel presets: Podcast, Rain, Cafe, Viral",
                        "Ad-free"
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (!isProMember) {
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFFFF9800), Color(0xFFFFCA28))
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Upgrade to Pro",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ You're on Pro",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Cancel anytime. Billed monthly.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    priceSuffix: String,
    isCurrent: Boolean,
    accentColor: Color,
    isHighlighted: Boolean = false,
    bullets: List<String>
) {
    val bg = if (isHighlighted) accentColor.copy(alpha = 0.08f)
             else MaterialTheme.colorScheme.surfaceContainer
    val borderColor = if (isHighlighted || isCurrent) accentColor
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isHighlighted || isCurrent) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isHighlighted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor
                            ) {
                                Text(
                                    text = "BEST VALUE",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "CURRENT",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                    Text(
                        text = priceSuffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
