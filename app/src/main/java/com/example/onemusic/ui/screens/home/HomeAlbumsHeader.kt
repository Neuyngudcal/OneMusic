package com.example.onemusic.ui.screens.home

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** Đầu trang Album: nút quay lại, tiêu đề + số album/bài, nút đổi chế độ xem (danh sách / lưới 2 / lưới 3). */
@Composable
internal fun HomeAlbumsHeader(
    albumCount: Int,
    trackCount: Int,
    albumViewMode: AlbumViewMode,
    onAlbumViewModeChange: (AlbumViewMode) -> Unit,
    isViewMenuExpanded: Boolean,
    onViewMenuExpandedChange: (Boolean) -> Unit,
    hazeState: HazeState,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Back Button
        ApexCircularGlassButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
            contentDescription = "Quay lại",
            onClick = onBack,
            size = 44.dp,
            iconSize = 20.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Album",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 22.sp
                )
            )
            Text(
                text = "$albumCount album • $trackCount bài hát",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.5.sp
                )
            )
        }

        // Circular Layout Switcher Button
        val currentModeIcon = when (albumViewMode) {
            AlbumViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
            AlbumViewMode.GRID_2 -> Icons.Rounded.GridView
            AlbumViewMode.GRID_3 -> Icons.Rounded.ViewModule
        }

        Box {
            ApexCircularGlassButton(
                icon = currentModeIcon,
                contentDescription = "Tùy chọn hiển thị",
                onClick = { onViewMenuExpandedChange(!isViewMenuExpanded) },
                size = 44.dp,
                iconSize = 22.dp
            )

            ApexDropdownMenu(
                expanded = isViewMenuExpanded,
                onDismissRequest = { onViewMenuExpandedChange(false) },
                hazeState = hazeState,
                width = 205.dp
            ) {
                ApexDropdownMenuItem(
                    text = "Danh sách",
                    icon = Icons.AutoMirrored.Rounded.ViewList,
                    trailingText = if (albumViewMode == AlbumViewMode.LIST) "✓" else null,
                    trailingColor = AppTheme.colors.accent,
                    textColor = if (albumViewMode == AlbumViewMode.LIST) AppTheme.colors.accent else AppTheme.colors.textPrimary,
                    onClick = {
                        onAlbumViewModeChange(AlbumViewMode.LIST)
                        onViewMenuExpandedChange(false)
                    }
                )
                ApexDropdownDivider()
                ApexDropdownMenuItem(
                    text = "Lưới 2 cột",
                    icon = Icons.Rounded.GridView,
                    trailingText = if (albumViewMode == AlbumViewMode.GRID_2) "✓" else null,
                    trailingColor = AppTheme.colors.accent,
                    textColor = if (albumViewMode == AlbumViewMode.GRID_2) AppTheme.colors.accent else AppTheme.colors.textPrimary,
                    onClick = {
                        onAlbumViewModeChange(AlbumViewMode.GRID_2)
                        onViewMenuExpandedChange(false)
                    }
                )
                ApexDropdownDivider()
                ApexDropdownMenuItem(
                    text = "Lưới 3 cột",
                    icon = Icons.Rounded.ViewModule,
                    trailingText = if (albumViewMode == AlbumViewMode.GRID_3) "✓" else null,
                    trailingColor = AppTheme.colors.accent,
                    textColor = if (albumViewMode == AlbumViewMode.GRID_3) AppTheme.colors.accent else AppTheme.colors.textPrimary,
                    onClick = {
                        onAlbumViewModeChange(AlbumViewMode.GRID_3)
                        onViewMenuExpandedChange(false)
                    }
                )
            }
        }
    }
}
