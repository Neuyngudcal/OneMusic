package com.example.onemusic.ui.screens.search

import com.example.onemusic.theme.AppTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.ApexPillBorderBrush
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.ShadowColor

/** Ô tìm kiếm ghim ở đầu màn hình. Nút "Tìm" trên bàn phím ẩn bàn phím rồi gọi [onSubmitSearch] (lưu từ khóa). */
@Composable
internal fun SearchInputBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    focusRequester: FocusRequester
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    @OptIn(ExperimentalFoundationApi::class)
    val noOpBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = localRect
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                // Prevent Compose from nudging/scrolling the search bar downwards on focus
            }
        }
    }

    // 1. Top Search Bar Header (Pinned rock-solid at the top)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp)
            .height(52.dp)
            .shadow(elevation = 8.dp, shape = PillShape, ambientColor = ShadowColor)
            .clip(PillShape)
            .background(AppTheme.colors.surface1.copy(alpha = 0.88f))
            .border(0.85.dp, ApexPillBorderBrush, PillShape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Tìm kiếm",
                tint = IvoryHigh,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Nghệ sĩ, bài hát, album...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textSecondary, // TextTertiary chỉ ~3:1, không đạt WCAG AA
                            fontSize = 15.sp
                        )
                    )
                }

                @OptIn(ExperimentalFoundationApi::class)
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = AppTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(AppTheme.colors.textPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    // Nút "Tìm" trên bàn phím: ẩn bàn phím và lưu từ khóa vào lịch sử
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        onSubmitSearch()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .bringIntoViewResponder(noOpBringIntoViewResponder)
                )
            }

            if (searchQuery.isNotEmpty()) {
                ApexCircularGlassButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Xóa tìm kiếm",
                    onClick = { onSearchQueryChange("") },
                    size = 32.dp,
                    iconSize = 16.dp,
                    modifier = Modifier.minimumInteractiveComponentSize(), // vùng chạm ≥ 48dp
                    iconTint = AppTheme.colors.textPrimary.copy(alpha = 0.85f),
                    backgroundColor = AppTheme.colors.surfaceActiveIndicator.copy(alpha = 0.60f)
                )
            }
        }
    }
}

/** Hàng tab lọc: Tất cả / Bài hát / Nghệ sĩ / Album. */
@Composable
internal fun SearchFilterTabsRow(selectedFilter: SearchFilterTab, onSelectFilter: (SearchFilterTab) -> Unit) {
    // 2. Category Filter Pills Row (Tất cả, Bài hát, Nghệ sĩ, Album)
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(SearchFilterTab.entries.toTypedArray(), key = { it.name }) { tab ->
            val isSelected = selectedFilter == tab
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.surface2,
                animationSpec = spring(stiffness = 500f),
                label = "tab_pill_bg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) Color.Transparent else AppTheme.colors.borderStrong,
                animationSpec = spring(stiffness = 500f),
                label = "tab_pill_border"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) AppTheme.colors.onInverse else AppTheme.colors.textSecondary,
                animationSpec = spring(stiffness = 500f),
                label = "tab_pill_text"
            )
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(PillShape)
                    .background(bgColor)
                    .border(0.8.dp, borderColor, PillShape)
                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                        onSelectFilter(tab)
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
