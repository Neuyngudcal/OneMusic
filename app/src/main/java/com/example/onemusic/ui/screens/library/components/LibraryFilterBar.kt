package com.example.onemusic.ui.screens.library.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.R
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.screens.library.LibraryViewMode
import com.example.onemusic.ui.screens.library.SongSortOption
import com.example.onemusic.ui.utils.apexBounceClick

/**
 * Filter and Sort Chips + Quick Action Row (Play All & Shuffle) + View Mode toggle.
 */
@Composable
fun LibraryFilterBar(
    currentSortOption: SongSortOption,
    sortAscending: Boolean,
    viewMode: LibraryViewMode,
    onSortOptionChange: (SongSortOption) -> Unit,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Sort Chips Row with View Mode Switch Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongSortOption.entries.forEach { option ->
                val isSelected = currentSortOption == option

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryIvory else SurfaceControl,
                    animationSpec = spring(stiffness = 500f),
                    label = "filter_bg_anim"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) CharcoalBlack else TextSecondary,
                    animationSpec = spring(stiffness = 500f),
                    label = "filter_text_anim"
                )

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(PillShape)
                        .background(bgColor)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) Color.Transparent else SurfaceBorderStrong,
                            shape = PillShape
                        )
                        .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                            onSortOptionChange(option)
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = textColor,
                                fontSize = 13.sp
                            )
                        )
                        if (isSelected && option != SongSortOption.ALL) {
                            Icon(
                                imageVector = if (sortAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                contentDescription = "Thứ tự sắp xếp",
                                tint = CharcoalBlack,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Quick View Mode Cycle Button (List -> Grid 2 -> Grid 3 -> List)
            val nextViewMode = when (viewMode) {
                LibraryViewMode.LIST -> LibraryViewMode.GRID_2
                LibraryViewMode.GRID_2 -> LibraryViewMode.GRID_3
                LibraryViewMode.GRID_3 -> LibraryViewMode.LIST
            }
            val viewModeIcon = when (viewMode) {
                LibraryViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
                LibraryViewMode.GRID_2 -> Icons.Rounded.GridView
                LibraryViewMode.GRID_3 -> Icons.Rounded.ViewModule
            }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(PillShape)
                    .background(SurfaceControl)
                    .border(width = 1.5.dp, color = SurfaceBorderStrong, shape = PillShape)
                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                        onViewModeChange(nextViewMode)
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = viewModeIcon,
                        contentDescription = "Đổi chế độ xem",
                        tint = PrimaryIvory,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = viewMode.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIvory,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // 2. Quick Action Row: Play All & Shuffle All Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play All Button (Primary Ivory Pill)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(PillShape)
                    .background(PrimaryIvory)
                    .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                        onPlayAll()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Phát tất cả",
                        tint = CharcoalBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Phát tất cả",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalBlack,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // Shuffle All Button
            ApexCircularGlassButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_widget_shuffle),
                contentDescription = "Trộn bài",
                onClick = onShuffleAll,
                size = 46.dp,
                iconSize = 22.dp,
                backgroundColor = PrimaryIvory,
                iconTint = CharcoalBlack
            )
        }
    }
}
