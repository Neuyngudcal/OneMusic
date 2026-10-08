package com.example.onemusic.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.local.HapticIntensity
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.TextSecondary
import androidx.compose.foundation.lazy.LazyListScope
import com.example.onemusic.haptics.ApexHapticEngine

/** Nhóm "Rung phản hồi": bật/tắt rung và chọn mức độ rung. */
internal fun LazyListScope.settingsHapticsSection(
    settings: AppSettings,
    settingsPreferences: SettingsPreferences,
    hapticEngine: ApexHapticEngine
) {
    // ================================================================
    // SECTION 3: PHẢN HỒI XÚC GIÁC (HAPTICS)
    // ================================================================
    item(key = "header_haptics") { SettingsSectionHeader("Rung phản hồi") }

    item(key = "section_card_haptics") {
        SettingsGroupCard {
            // Haptic Feedback Toggle
            SettingsToggleRow(
                icon = Icons.Rounded.Vibration,
                title = "Rung khi chạm",
                subtitle = "Rung nhẹ khi bấm nút và vuốt",
                checked = settings.isHapticFeedbackEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isHapticFeedbackEnabled = isChecked) }
                }
            )

            // Haptic Intensity Selector (Visible when enabled)
            AnimatedVisibility(
                visible = settings.isHapticFeedbackEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Mức độ rung",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HapticIntensity.entries.forEach { intensity ->
                            val isSelected = settings.hapticIntensity == intensity
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PillShape)
                                    .background(if (isSelected) PrimaryIvory else SurfaceControl)
                                    .border(1.5.dp, if (isSelected) Color.Transparent else SurfaceBorderStrong, PillShape)
                                    .apexBounceClick(scaleDown = 0.94f, enableHaptic = false) {
                                        settingsPreferences.updateSettings {
                                            it.copy(hapticIntensity = intensity)
                                        }
                                        hapticEngine.performCrispTap()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = intensity.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CharcoalBlack else PrimaryIvory,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
