package com.example.onemusic.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ============================================================================
// Samsung One UI Continuous Curvature (Squircle & Pill Geometry)
// ============================================================================
val SquircleSmall = RoundedCornerShape(12.dp)       // Badges, tags, miniature chips
val SquircleMedium = RoundedCornerShape(20.dp)      // Track item cards, sub-buttons
val SquircleLarge = RoundedCornerShape(26.dp)       // Grouped Card Containers, Vibe Mixes, Dialogs
val SquircleHero = RoundedCornerShape(36.dp)        // Hero Album Artwork in Now Playing
val SquircleExtraLarge = SquircleHero               // Alias
val PillShape = RoundedCornerShape(50)              // Now Bar, Bottom Bar, Filter Pills, Action Buttons
val CircleAvatarShape = CircleShape
