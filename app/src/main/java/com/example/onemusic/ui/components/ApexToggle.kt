package com.example.onemusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.example.onemusic.haptics.rememberApexHaptics
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.SurfaceActive
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary

/**
 * OneMusic Apex Prism Signature Switch Toggle with Hardware Tactile Haptics
 */
@Composable
fun ApexToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticEngine = rememberApexHaptics()
    val currentView = LocalView.current

    val trackColor by animateColorAsState(
        targetValue = if (checked) Brand else SurfaceActive,
        label = "trackColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(PillShape)
            .background(trackColor)
            .border(BorderStroke(1.dp, if (checked) Brand else SurfaceDivider), PillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                try {
                    hapticEngine.performConfirmation(scale = 0.45f, fallbackView = currentView)
                } catch (_: Exception) {}
                onCheckedChange(!checked)
            }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(TextPrimary)
        )
    }
}


