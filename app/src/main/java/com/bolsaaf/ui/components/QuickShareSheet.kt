package com.bolsaaf.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Phase 3 — Quick share targets.
 *
 * Compact bottom sheet shown when the user taps "Share" on a cleaned reel.
 * Shows one-tap buttons for Instagram, WhatsApp, YouTube (greyed out if the
 * package isn't installed), plus a "More" chooser fallback.
 *
 * All callbacks receive the fileName — MainActivity resolves the File +
 * FileProvider uri and launches the targeted intent.
 */

const val PACKAGE_INSTAGRAM = "com.instagram.android"
const val PACKAGE_WHATSAPP = "com.whatsapp"
const val PACKAGE_YOUTUBE = "com.google.android.youtube"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickShareSheet(
    fileName: String,
    onDismiss: () -> Unit,
    onShareToPackage: (fileName: String, packageName: String) -> Unit,
    onShareGeneric: (fileName: String) -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val igInstalled = isPackageInstalled(pm, PACKAGE_INSTAGRAM)
    val waInstalled = isPackageInstalled(pm, PACKAGE_WHATSAPP)
    val ytInstalled = isPackageInstalled(pm, PACKAGE_YOUTUBE)

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Share to",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                ShareTarget(
                    icon = Icons.Filled.Camera,
                    label = "Instagram",
                    enabled = igInstalled,
                    onClick = {
                        onShareToPackage(fileName, PACKAGE_INSTAGRAM)
                        onDismiss()
                    }
                )
                ShareTarget(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "WhatsApp",
                    enabled = waInstalled,
                    onClick = {
                        onShareToPackage(fileName, PACKAGE_WHATSAPP)
                        onDismiss()
                    }
                )
                ShareTarget(
                    icon = Icons.Filled.PlayCircle,
                    label = "YouTube",
                    enabled = ytInstalled,
                    onClick = {
                        onShareToPackage(fileName, PACKAGE_YOUTUBE)
                        onDismiss()
                    }
                )
                ShareTarget(
                    icon = Icons.Filled.MoreHoriz,
                    label = "More",
                    enabled = true,
                    onClick = {
                        onShareGeneric(fileName)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShareTarget(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (enabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .width(72.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        if (!enabled) {
            Text(
                text = "Not installed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun isPackageInstalled(pm: PackageManager, pkg: String): Boolean = try {
    @Suppress("DEPRECATION")
    pm.getPackageInfo(pkg, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}
