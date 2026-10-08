package com.example.onemusic.ui.screens.library.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.screens.library.LibraryTab
import com.example.onemusic.ui.utils.apexBounceClick

/**
 * One UI 8.5 Tab Row for Library (Songs, Albums, Artists, Playlists).
 * Only the selected tab gets a pill; the others are plain text. Horizontal scroll is kept
 * only as a fallback for large font scales — at normal size all 4 tabs fit on screen.
 */
@Composable
fun LibraryTabRow(
    currentTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LibraryTab.entries.forEach { tab ->
            val isSelected = currentTab == tab

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) AppTheme.colors.textPrimary else Color.Transparent,
                animationSpec = spring(stiffness = 500f),
                label = "tab_bg_anim"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) AppTheme.colors.onInverse else AppTheme.colors.textSecondary,
                animationSpec = spring(stiffness = 500f),
                label = "tab_text_anim"
            )

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(PillShape)
                    .background(bgColor)
                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                        onTabSelected(tab)
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
