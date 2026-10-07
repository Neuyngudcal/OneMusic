package com.example.onemusic.ui.screens.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexDropdownDivider
import com.example.onemusic.ui.components.ApexDropdownMenu
import com.example.onemusic.ui.components.ApexDropdownMenuItem
import com.example.onemusic.ui.screens.library.LibraryViewMode
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

/**
 * One UI 8.5 Collapsible Large Title Header for LibraryScreen with options menu.
 */
@Composable
fun LibraryCollapsibleHeader(
    title: String = "Thư viện",
    subtitle: String,
    viewMode: LibraryViewMode,
    isMultiSelectMode: Boolean,
    hazeState: HazeState,
    onViewModeChange: (LibraryViewMode) -> Unit,
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
                    color = PrimaryIvory,
                    fontSize = 32.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            )
        }

        // Top-Right Action Menu (⋮) with Integrated View Mode Switcher
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
                // Section: Chế độ hiển thị
                Text(
                    text = "CHẾ ĐỘ HIỂN THỊ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Danh sách (LIST)
                    val isList = viewMode == LibraryViewMode.LIST
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isList) PrimaryIvory else Color.Transparent)
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                onViewModeChange(LibraryViewMode.LIST)
                                isTopMenuOpen = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ViewList,
                                contentDescription = "Danh sách",
                                tint = if (isList) CharcoalBlack else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Danh sách",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isList) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isList) CharcoalBlack else TextSecondary,
                                    fontSize = 10.5.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    // 2. Lưới 2 cột (GRID_2)
                    val isGrid2 = viewMode == LibraryViewMode.GRID_2
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isGrid2) PrimaryIvory else Color.Transparent)
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                onViewModeChange(LibraryViewMode.GRID_2)
                                isTopMenuOpen = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = "Lưới 2",
                                tint = if (isGrid2) CharcoalBlack else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lưới 2",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isGrid2) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isGrid2) CharcoalBlack else TextSecondary,
                                    fontSize = 10.5.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    // 3. Lưới 3 cột (GRID_3)
                    val isGrid3 = viewMode == LibraryViewMode.GRID_3
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isGrid3) PrimaryIvory else Color.Transparent)
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                onViewModeChange(LibraryViewMode.GRID_3)
                                isTopMenuOpen = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ViewModule,
                                contentDescription = "Lưới 3",
                                tint = if (isGrid3) CharcoalBlack else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lưới 3",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isGrid3) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isGrid3) CharcoalBlack else TextSecondary,
                                    fontSize = 10.5.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                ApexDropdownDivider()

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
