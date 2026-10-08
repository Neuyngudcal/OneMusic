package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.onemusic.theme.ApexCyan
import com.example.onemusic.theme.ApexIndigo
import com.example.onemusic.theme.Brand

private val AuroraColorEasing = CubicBezierEasing(0.25f, 0.10f, 0.25f, 1.00f)

/**
 * Dynamic Ambient Mesh Gradient for Fullscreen Player & Cards
 * Dimmed according to One UI 8.5 specs (65-75% dim) for optimal text contrast.
 */
@Composable
fun DynamicMeshBackground(
    primaryColor: Color = Brand,
    secondaryColor: Color = ApexIndigo,
    tertiaryColor: Color = ApexCyan,
    modifier: Modifier = Modifier
) {
    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor.copy(alpha = 0.28f),
        animationSpec = tween(durationMillis = 600, easing = AuroraColorEasing),
        label = "primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor.copy(alpha = 0.20f),
        animationSpec = tween(durationMillis = 600, easing = AuroraColorEasing),
        label = "secondary"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = tertiaryColor.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 600, easing = AuroraColorEasing),
        label = "tertiary"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(animatedPrimary, Color.Transparent),
                        radius = 1200f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(animatedTertiary, Color.Transparent),
                        radius = 800f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            animatedSecondary,
                            AppTheme.colors.background.copy(alpha = 0.88f),
                            AppTheme.colors.background
                        )
                    )
                )
        )
    }
}

