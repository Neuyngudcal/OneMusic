package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.SoundPreset
import com.example.onemusic.haptics.rememberApexHaptics
import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.ui.utils.apexBounceClick
import kotlin.math.roundToInt
import com.example.onemusic.theme.PillShape

private val FREQUENCY_LABELS = listOf(
    "32Hz", "64Hz", "125Hz", "250Hz", "500Hz",
    "1kHz", "2kHz", "4kHz", "8kHz"
)

@Composable
fun ApexEqualizerDialog(
    audioEffectManager: AudioEffectManager,
    onDismiss: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val settings by audioEffectManager.settings.collectAsState()
    val hapticEngine = rememberApexHaptics()
    val view = LocalView.current
    val scrollState = rememberScrollState()

    ApexDialogContainer(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header: Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.accent.copy(alpha = 0.15f))
                        .border(0.8.dp, AppTheme.colors.accent.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bộ Chỉnh Âm & DSP",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "SoundAlive Hardware Audio Engine",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Preset Selection Pills (Horizontal Scrollable)
            val presetScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(presetScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoundPreset.entries.forEach { preset ->
                    val isSelected = settings.preset == preset
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.surfaceControl,
                        animationSpec = spring(stiffness = 500f),
                        label = "preset_bg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) AppTheme.colors.onInverse else AppTheme.colors.textSecondary,
                        animationSpec = spring(stiffness = 500f),
                        label = "preset_text"
                    )

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(PillShape)
                            .background(bgColor)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Color.Transparent else AppTheme.colors.borderStrong,
                                shape = PillShape
                            )
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                audioEffectManager.applyPreset(preset)
                                try {
                                    hapticEngine.performGearTick(scale = 0.25f, fallbackView = view)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = textColor,
                                fontSize = 12.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Bass Boost Dedicated Slider Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppTheme.colors.surface2)
                    .border(0.8.dp, AppTheme.colors.hairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tăng Âm Trầm (Bass Boost)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppTheme.colors.textPrimary,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                        Text(
                            text = if (settings.bassBoostLevel > 0) "${settings.bassBoostLevel}/10" else "Tắt",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (settings.bassBoostLevel > 0) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                                fontSize = 13.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApexSlider(
                        value = settings.bassBoostLevel.toFloat(),
                        onValueChange = { newVal ->
                            val level = newVal.roundToInt()
                            if (level != settings.bassBoostLevel) {
                                audioEffectManager.updateBassBoost(level)
                                try {
                                    hapticEngine.performGearTick(scale = 0.20f, fallbackView = view)
                                } catch (_: Exception) {}
                            }
                        },
                        valueRange = 0f..10f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. 9-Band Equalizer Card (-10dB to +10dB)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppTheme.colors.surface2)
                    .border(0.8.dp, AppTheme.colors.hairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Cần Gạt Tần Số Âm Thanh",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val bands = settings.equalizerBands
                    FREQUENCY_LABELS.forEachIndexed { index, label ->
                        val gain = bands.getOrElse(index) { 0f }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 11.5.sp
                                ),
                                modifier = Modifier.width(46.dp)
                            )
                            ApexSlider(
                                value = gain,
                                onValueChange = { newGain ->
                                    val rounded = (newGain * 2).roundToInt() / 2f
                                    audioEffectManager.updateEqualizerBand(index, rounded)
                                },
                                valueRange = -10f..10f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                            )
                            Text(
                                text = String.format("%+.1fdB", gain),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (gain != 0f) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.width(48.dp)
                            )
                        }
                        if (index < FREQUENCY_LABELS.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(AppTheme.colors.divider.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Action Buttons: Reset to Flat & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reset Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(PillShape)
                        .background(AppTheme.colors.surfaceControl)
                        .border(1.5.dp, AppTheme.colors.borderStrong, PillShape)
                        .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                            audioEffectManager.resetToFlat()
                            try {
                                hapticEngine.performSpringLatch(scale = 0.35f, fallbackView = view)
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = AppTheme.colors.textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mặc định",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(PillShape)
                        .background(AppTheme.colors.textPrimary)
                        .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Xong",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.onInverse,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}
