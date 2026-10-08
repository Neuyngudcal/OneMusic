package com.example.onemusic.ui.screens.settings

import com.example.onemusic.theme.AppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Refresh
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.local.SettingsPreferences
import androidx.compose.foundation.lazy.LazyListScope

/** Nhóm "Thư viện & bộ nhớ": quét nhạc, thư mục, dọn trùng lặp, xóa bộ nhớ đệm và lịch sử tìm kiếm. */
internal fun LazyListScope.settingsLibrarySection(
    settings: AppSettings,
    settingsPreferences: SettingsPreferences,
    onOpenFolders: () -> Unit,
    onOpenDuplicateCleaner: () -> Unit,
    onRescan: () -> Unit,
    onClearArtistCache: () -> Unit,
    onClearMotionCache: () -> Unit,
    onClearSearchHistory: () -> Unit
) {
    // ================================================================
    // SECTION 4: THƯ VIỆN & BỘ NHỚ (LIBRARY & STORAGE)
    // ================================================================
    item(key = "header_library") { SettingsSectionHeader("Thư viện & bộ nhớ") }

    item(key = "section_card_library") {
        SettingsGroupCard {
            // Filter Short Audio Files
            SettingsToggleRow(
                icon = Icons.Rounded.MusicNote,
                title = "Bỏ qua tệp ngắn",
                subtitle = "Không quét tệp dưới 30 giây (nhạc chuông, ghi âm…)",
                checked = settings.filterShortAudio,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(filterShortAudio = isChecked) }
                }
            )

            SettingsDivider()

            // Auto-scan on launch
            SettingsToggleRow(
                icon = Icons.Rounded.Refresh,
                title = "Quét khi mở ứng dụng",
                subtitle = "Tìm bài hát mới mỗi lần mở OneMusic",
                checked = settings.autoScanOnLaunch,
                onCheckedChange = { isChecked ->
                    settingsPreferences.updateSettings { it.copy(autoScanOnLaunch = isChecked) }
                }
            )

            SettingsDivider()

            // Folder Manager Navigation
            SettingsActionRow(
                icon = Icons.Rounded.Folder,
                title = "Thư mục nhạc",
                subtitle = "Chọn thư mục để quét nhạc",
                onClick = onOpenFolders
            )

            SettingsDivider()

            // Duplicate Cleaner
            SettingsActionRow(
                icon = Icons.Rounded.CopyAll,
                title = "Dọn bài trùng lặp",
                subtitle = "Tìm và dọn các bản nhạc bị trùng",
                onClick = onOpenDuplicateCleaner
            )

            SettingsDivider()

            // Rescan Library Now
            SettingsActionRow(
                icon = Icons.Rounded.Refresh,
                title = "Quét lại thư viện",
                subtitle = "Cập nhật danh sách bài hát và thông tin bài",
                // Trước đây gọi onRescan() không kiểm tra quyền → không có quyền thì im lặng không làm gì
                onClick = { onRescan() }
            )

            SettingsDivider()

            // Clear Artist Images Cache
            SettingsActionRow(
                icon = Icons.Rounded.DeleteOutline,
                title = "Xóa ảnh nghệ sĩ đã tải",
                subtitle = "Ảnh sẽ được tải lại khi cần",
                titleColor = AppTheme.colors.danger,
                onClick = onClearArtistCache
            )

            SettingsDivider()

            // Clear Motion Artwork Cache
            SettingsActionRow(
                icon = Icons.Rounded.DeleteOutline,
                title = "Xóa bìa động đã tải",
                subtitle = "Giải phóng dung lượng; bìa sẽ được tải lại khi cần",
                titleColor = AppTheme.colors.danger,
                onClick = onClearMotionCache
            )

            SettingsDivider()

            // Clear Search History
            SettingsActionRow(
                icon = Icons.Rounded.DeleteOutline,
                title = "Xóa lịch sử tìm kiếm",
                subtitle = "Xóa các từ khóa tìm kiếm gần đây",
                titleColor = AppTheme.colors.danger,
                onClick = onClearSearchHistory
            )
        }
    }
}
