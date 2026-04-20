package com.reelvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reelvoice.audio.ModePreset
import com.reelvoice.audio.ModePresets
import com.reelvoice.ui.theme.BrandGradient

/**
 * Horizontal strip of "Mode" tiles shown on Home under the primary ActionGrid.
 * Tap a tile → host sets the cleaning preset + background + BG mix volume,
 * then takes the user to upload/record. Lightweight hook into the existing
 * Add-Vibe flow; no new server endpoints.
 */
@Composable
fun ModePresetStrip(
    modifier: Modifier = Modifier,
    presets: List<ModePreset> = ModePresets.ALL,
    onPick: (ModePreset) -> Unit,
) {
    if (presets.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Modes",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { preset ->
                ModePresetTile(preset = preset, onClick = { onPick(preset) })
            }
        }
    }
}

@Composable
private fun ModePresetTile(
    preset: ModePreset,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandGradient.BrandSoft)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preset.emoji,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = preset.displayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
