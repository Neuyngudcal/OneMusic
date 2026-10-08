package com.example.onemusic.ui.screens.settings

import com.example.onemusic.theme.AppTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.ui.components.ApexSlider
import kotlin.math.roundToInt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.lazy.LazyListScope

/** Nhóm "Âm thanh & phát nhạc": bộ chỉnh âm, ReplayGain, chống vỡ tiếng, gapless, crossfade, tai nghe. */
internal fun LazyListScope.settingsAudioSection(
    settings: AppSettings,
    settingsPreferences: SettingsPreferences,
    // Giá trị Crossfade tạm trong lúc kéo; truyền lambda để chỉ hàng thanh trượt đọc nó
    crossfadeDraft: () -> Float,
    onCrossfadeDraftChange: (Float) -> Unit,
    onOpenEqualizer: () -> Unit
) {
    // ================================================================
    // SECTION 1: ÂM THANH & PHÁT NHẠC (AUDIO & PLAYBACK)
    // ================================================================
    item(key = "header_audio") { SettingsSectionHeader("Âm thanh & phát nhạc") }

    item(key = "section_card_audio") {
        SettingsGroupCard {
            // Equalizer & SoundAlive DSP
            SettingsActionRow(
                icon = Icons.Rounded.Tune,
                title = "Bộ chỉnh âm",
                subtitle = "Chỉnh 9 dải tần, tăng bass và chọn cấu hình SoundAlive",
                onClick = onOpenEqualizer
            )

            SettingsDivider()

            // ReplayGain (EBU R128)
            SettingsToggleRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Cân bằng âm lượng",
                subtitle = "Giữ các bài to nhỏ đều nhau (ReplayGain / EBU R128)",
                checked = settings.isReplayGainEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isReplayGainEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Headroom Limiter
            SettingsToggleRow(
                icon = Icons.Rounded.Security,
                title = "Chống vỡ tiếng",
                subtitle = "Tránh rè tiếng khi tăng âm lượng bài hát (Headroom Limiter)",
                checked = settings.isHeadroomLimiterEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isHeadroomLimiterEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Gapless Playback
            SettingsToggleRow(
                icon = Icons.Rounded.MusicNote,
                title = "Phát liền mạch",
                subtitle = "Chuyển bài không có khoảng lặng (Gapless)",
                checked = settings.isGaplessPlaybackEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isGaplessPlaybackEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Crossfade
            SettingsToggleRow(
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                title = "Chuyển bài mượt",
                subtitle = "Hòa dần cuối bài cũ vào đầu bài mới (Crossfade)",
                checked = settings.isCrossfadeEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isCrossfadeEnabled = isChecked) }
                }
            )

            // Crossfade Duration Slider (Visible when enabled)
            AnimatedVisibility(
                visible = settings.isCrossfadeEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thời gian làm mờ",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppTheme.colors.textSecondary,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "${crossfadeDraft().roundToInt()} giây",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.accent,
                                fontSize = 14.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    ApexSlider(
                        value = crossfadeDraft(),
                        // Trong lúc kéo chỉ đổi giá trị tạm; thả tay mới ghi vào bộ nhớ
                        onValueChange = onCrossfadeDraftChange,
                        onValueChangeFinished = {
                            settingsPreferences.updateSettings {
                                it.copy(crossfadeDurationSeconds = crossfadeDraft().roundToInt())
                            }
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SettingsDivider()

            // Pause on Headset Disconnect
            SettingsToggleRow(
                icon = Icons.Rounded.Headphones,
                title = "Dừng khi rút tai nghe",
                subtitle = "Tạm dừng khi ngắt tai nghe có dây hoặc Bluetooth",
                checked = settings.pauseOnHeadsetDisconnect,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(pauseOnHeadsetDisconnect = isChecked) }
                }
            )

            SettingsDivider()

            // Resume on Headset Connect
            SettingsToggleRow(
                icon = Icons.Rounded.Headphones,
                title = "Phát khi cắm tai nghe",
                subtitle = "Phát tiếp khi kết nối lại tai nghe hoặc Bluetooth",
                checked = settings.resumeOnHeadsetConnect,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(resumeOnHeadsetConnect = isChecked) }
                }
            )
        }
    }
}
