package com.bolsaaf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bolsaaf.BuildConfig
import com.bolsaaf.ui.theme.BackgroundCard
import com.bolsaaf.ui.theme.NavUnselected
import com.bolsaaf.ui.theme.SliderTrackStrong
import com.bolsaaf.ui.theme.ThemeRed

/**
 * 5-tab bottom navigation shared by app screens.
 *
 * Tab indices:
 *   0 Home, 1 Live, 2 History, 3 Profile, 4 Lab.
 *
 * Kept in ui.components so each screen imports a single source of truth.
 */
@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .border(
                1.dp,
                SliderTrackStrong.copy(alpha = 0.28f),
                RoundedCornerShape(28.dp)
            ),
        color = BackgroundCard,
        shadowElevation = 16.dp,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NavItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            NavItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Mic,
                label = "Live",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            NavItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.History,
                label = "History",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
            NavItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = selectedTab == 3,
                onClick = { onTabSelected(3) }
            )
            if (BuildConfig.DEBUG) {
                NavItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Build,
                    label = "Lab",
                    badge = "DEV",
                    isSelected = selectedTab == 4,
                    onClick = { onTabSelected(4) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    badge: String? = null,
    onClick: () -> Unit = {}
) {
    val activeColor by animateColorAsState(
        targetValue = if (isSelected) ThemeRed else NavUnselected,
        animationSpec = spring(stiffness = 700f),
        label = "navColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) ThemeRed.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = spring(stiffness = 700f),
        label = "navContainer"
    )
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 22.dp else 20.dp,
        animationSpec = spring(stiffness = 700f),
        label = "navIconSize"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.88f,
        animationSpec = spring(stiffness = 700f),
        label = "navTextAlpha"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = activeColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = activeColor,
                    modifier = Modifier.size(iconSize)
                )
                if (badge != null) {
                    // Small dot in top-right corner of the icon, signalling
                    // a non-primary tab (e.g. internal/dev). Visual cue + the
                    // badge text below carries the meaning for screen readers.
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp, end = 0.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ThemeRed)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = activeColor.copy(alpha = textAlpha),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeRed,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
