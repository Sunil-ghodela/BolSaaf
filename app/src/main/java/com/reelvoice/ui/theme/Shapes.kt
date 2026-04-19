package com.reelvoice.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Design 3 Shape System
val ReelVoiceShapes = Shapes(
    // Extra Small - 4dp - Small components (text field, snackbar)
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - 8dp - Smaller containers
    small = RoundedCornerShape(8.dp),
    
    // Medium - 12dp - Cards, buttons, chips
    medium = RoundedCornerShape(12.dp),
    
    // Large - 16dp - Dialogs, larger containers
    large = RoundedCornerShape(16.dp),
    
    // Extra Large - 28dp - Bottom sheets, full-screen dialogs
    extraLarge = RoundedCornerShape(28.dp)
)
