package com.example.onemusic.ui.screens.settings

import com.example.onemusic.theme.AppTheme
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.ThemeMode
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.utils.apexBounceClick
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.local.SettingsPreferences
import androidx.compose.foundation.lazy.LazyListScope

/** Nhóm "Giao diện": chế độ Sáng/Tối/Theo hệ thống, nền màu động, bìa động, lời bài hát, ảnh nghệ sĩ, màn hình, thanh hệ thống. */
internal fun LazyListScope.settingsDisplaySection(
    settings: AppSettings,
    settingsPreferences: SettingsPreferences
) {
    // ================================================================
    // SECTION 2: GIAO DIỆN & HIỂN THỊ (DISPLAY & UI)
    // ================================================================
    item(key = "header_ui") { SettingsSectionHeader("Giao diện") }

    item(key = "section_card_ui") {
        SettingsGroupCard {
            // Chế độ giao diện: Sáng (mặc định) / Tối / Theo hệ thống
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Chế độ giao diện",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = settings.themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PillShape)
                                .background(if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.surfaceControl)
                                .border(1.5.dp, if (isSelected) Color.Transparent else AppTheme.colors.borderStrong, PillShape)
                                .apexBounceClick(scaleDown = 0.94f, enableHaptic = false) {
                                    settingsPreferences.updateSettings { it.copy(themeMode = mode) }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AppTheme.colors.onInverse else AppTheme.colors.textPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            SettingsDivider()

            // Dynamic Mesh Background
            SettingsToggleRow(
                icon = Icons.Rounded.Palette,
                title = "Nền màu động",
                subtitle = "Nền chuyển màu theo ảnh bìa; tắt để dùng nền đen",
                checked = settings.isDynamicMeshBackgroundEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isDynamicMeshBackgroundEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Motion Artwork (Apple Music)
            SettingsToggleRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Bìa động",
                subtitle = "Phát video bìa cho bài hát có hỗ trợ",
                checked = settings.isMotionArtworkEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isMotionArtworkEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Auto Motion Artwork Background Scan
            SettingsToggleRow(
                icon = Icons.Rounded.Refresh,
                title = "Tải trước bìa động",
                subtitle = "Tải bìa động cho cả thư viện; tắt để tiết kiệm dữ liệu, pin",
                checked = settings.isAutoMotionScanEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isAutoMotionScanEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Online Lyrics (LRCLIB)
            SettingsToggleRow(
                icon = Icons.Rounded.MusicNote,
                title = "Tải lời bài hát",
                subtitle = "Tự tải lời từ Internet khi tệp nhạc không có lời",
                checked = settings.isOnlineLyricsEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isOnlineLyricsEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Auto Download Artist Images
            SettingsToggleRow(
                icon = Icons.Rounded.Person,
                title = "Tải ảnh nghệ sĩ",
                subtitle = "Tự tải ảnh ca sĩ từ Internet",
                checked = settings.isAutoDownloadArtistImagesEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isAutoDownloadArtistImagesEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Keep Screen On
            SettingsToggleRow(
                icon = Icons.Rounded.Fullscreen,
                title = "Giữ màn hình sáng",
                subtitle = "Không tắt màn hình khi đang mở màn Đang phát",
                checked = settings.isKeepScreenOnEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isKeepScreenOnEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Hi-Res Badge
            SettingsToggleRow(
                icon = Icons.Rounded.Check,
                title = "Huy hiệu Hi-Res",
                subtitle = "Đánh dấu bài chất lượng cao (FLAC, 24-bit…)",
                checked = settings.isHiResBadgeEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isHiResBadgeEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Hide Status Bar
            SettingsToggleRow(
                icon = Icons.Rounded.Fullscreen,
                title = "Ẩn thanh trạng thái",
                subtitle = "Ẩn giờ, pin và thông báo ở đỉnh màn hình",
                checked = settings.isHideStatusBarEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isHideStatusBarEnabled = isChecked) }
                }
            )

            SettingsDivider()

            // Hide Navigation Bar
            SettingsToggleRow(
                icon = Icons.Rounded.Navigation,
                title = "Ẩn thanh điều hướng",
                subtitle = "Ẩn thanh ở đáy màn hình; vuốt lên từ cạnh dưới để hiện lại",
                checked = settings.isHideNavigationBarEnabled,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(isHideNavigationBarEnabled = isChecked) }
                }
            )
        }
    }
}
