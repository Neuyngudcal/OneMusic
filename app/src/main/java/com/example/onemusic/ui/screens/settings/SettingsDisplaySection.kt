package com.example.onemusic.ui.screens.settings

import androidx.compose.material.icons.Icons
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

/** Nhóm "Giao diện": nền màu động, bìa động, lời bài hát, ảnh nghệ sĩ, màn hình, thanh hệ thống. */
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
