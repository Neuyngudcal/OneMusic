package com.example.onemusic.ui.screens.library.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.R
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexDropdownMenu
import com.example.onemusic.ui.components.ApexDropdownMenuItem
import com.example.onemusic.ui.screens.library.LibraryViewMode
import com.example.onemusic.ui.screens.library.SongSortOption
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

private val ToolbarButtonSize = 40.dp

/**
 * Single-row library toolbar: sort dropdown on the left, view mode / shuffle / play on the right.
 * Sort and play controls are optional so tabs without them (Albums) only show the view mode button.
 */
@Composable
fun LibraryFilterBar(
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    currentSortOption: SongSortOption? = null,
    sortAscending: Boolean = true,
    onSortOptionChange: (SongSortOption) -> Unit = {},
    onPlayAll: (() -> Unit)? = null,
    onShuffleAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 20.dp, top = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentSortOption != null) {
            SortDropdownButton(
                currentSortOption = currentSortOption,
                sortAscending = sortAscending,
                hazeState = hazeState,
                onSortOptionChange = onSortOptionChange
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // View mode toggle: List <-> Grid 2
        val nextViewMode = when (viewMode) {
            LibraryViewMode.LIST -> LibraryViewMode.GRID_2
            LibraryViewMode.GRID_2 -> LibraryViewMode.LIST
        }
        val viewModeIcon = when (viewMode) {
            LibraryViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
            LibraryViewMode.GRID_2 -> Icons.Rounded.GridView
        }
        ApexCircularGlassButton(
            icon = viewModeIcon,
            contentDescription = "Chế độ xem: ${viewMode.title}. Chạm để đổi",
            onClick = { onViewModeChange(nextViewMode) },
            size = ToolbarButtonSize,
            iconSize = 20.dp,
            backgroundColor = AppTheme.colors.surfaceControl,
            iconTint = AppTheme.colors.textPrimary,
            elevation = 0.dp
        )

        if (onShuffleAll != null) {
            ApexCircularGlassButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_widget_shuffle),
                contentDescription = "Trộn bài",
                onClick = onShuffleAll,
                size = ToolbarButtonSize,
                iconSize = 20.dp,
                backgroundColor = AppTheme.colors.surfaceControl,
                iconTint = AppTheme.colors.textPrimary,
                elevation = 0.dp
            )
        }

        if (onPlayAll != null) {
            ApexCircularGlassButton(
                icon = Icons.Rounded.PlayArrow,
                contentDescription = "Phát tất cả",
                onClick = onPlayAll,
                size = ToolbarButtonSize,
                iconSize = 24.dp,
                backgroundColor = AppTheme.colors.textPrimary,
                iconTint = AppTheme.colors.onInverse,
                elevation = 0.dp
            )
        }
    }
}

/**
 * Text button showing the current sort ("Tên A-Z ↑ ▾"). Picking the active option again flips the direction.
 */
@Composable
private fun SortDropdownButton(
    currentSortOption: SongSortOption,
    sortAscending: Boolean,
    hazeState: HazeState?,
    onSortOptionChange: (SongSortOption) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .height(ToolbarButtonSize)
                .clip(RoundedCornerShape(12.dp))
                .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) { isMenuOpen = true }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentSortOption.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 14.sp
                )
            )
            if (currentSortOption != SongSortOption.ALL) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (sortAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = if (sortAscending) "Tăng dần" else "Giảm dần",
                    tint = AppTheme.colors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Chọn cách sắp xếp",
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        ApexDropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
            hazeState = hazeState,
            width = 220.dp,
            transformOrigin = TransformOrigin(0.1f, 0.05f)
        ) {
            SongSortOption.entries.forEach { option ->
                val isSelected = option == currentSortOption
                ApexDropdownMenuItem(
                    text = option.title,
                    textColor = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                    trailingText = when {
                        !isSelected -> null
                        option == SongSortOption.ALL -> "✓"
                        sortAscending -> "↑"
                        else -> "↓"
                    },
                    trailingColor = AppTheme.colors.accent,
                    onClick = {
                        isMenuOpen = false
                        onSortOptionChange(option)
                    }
                )
            }
        }
    }
}
