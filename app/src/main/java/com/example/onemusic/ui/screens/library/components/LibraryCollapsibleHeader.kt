package com.example.onemusic.ui.screens.library.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexDropdownDivider
import com.example.onemusic.ui.components.ApexDropdownMenu
import com.example.onemusic.ui.components.ApexDropdownMenuItem
import dev.chrisbanes.haze.HazeState

/**
 * One UI 8.5 Collapsible Large Title Header for LibraryScreen with options menu.
 */
@Composable
fun LibraryCollapsibleHeader(
    title: String = "Thư viện",
    subtitle: String,
    isMultiSelectMode: Boolean,
    hazeState: HazeState,
    onToggleMultiSelect: () -> Unit,
    onRescan: () -> Unit,
    onImportPlaylistM3u: (() -> Unit)? = null,
    onOpenFolders: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTopMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 32.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            )
        }

        // Top-Right Action Menu (⋮)
        Box {
            ApexCircularGlassButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = "Tùy chọn thư viện",
                onClick = { isTopMenuOpen = true },
                size = 40.dp,
                iconSize = 22.dp
            )

            ApexDropdownMenu(
                expanded = isTopMenuOpen,
                onDismissRequest = { isTopMenuOpen = false },
                hazeState = hazeState,
                width = 250.dp
            ) {
                // Thao tác
                ApexDropdownMenuItem(
                    text = if (isMultiSelectMode) "Thoát chọn nhiều" else "Chọn nhiều bài hát",
                    icon = Icons.Rounded.SelectAll,
                    onClick = {
                        isTopMenuOpen = false
                        onToggleMultiSelect()
                    }
                )
                ApexDropdownDivider()
                ApexDropdownMenuItem(
                    text = "Quét lại bài hát",
                    icon = Icons.Rounded.Refresh,
                    onClick = {
                        isTopMenuOpen = false
                        onRescan()
                    }
                )
                if (onImportPlaylistM3u != null) {
                    ApexDropdownMenuItem(
                        text = "Nhập danh sách phát (.m3u8)",
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        onClick = {
                            isTopMenuOpen = false
                            onImportPlaylistM3u()
                        }
                    )
                }
                ApexDropdownMenuItem(
                    text = "Quản lý thư mục",
                    icon = Icons.Rounded.MusicNote,
                    onClick = {
                        isTopMenuOpen = false
                        onOpenFolders()
                    }
                )
            }
        }
    }
}
