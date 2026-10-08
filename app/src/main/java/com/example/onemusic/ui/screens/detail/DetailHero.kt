package com.example.onemusic.ui.screens.detail

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.onemusic.R
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexHiResBadge
import com.example.onemusic.ui.utils.ShowSnackbar
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.ui.utils.formatTotalDuration

/**
 * LAYER 1: nền hero 520dp (ảnh bìa tràn viền / icon tim, nghệ sĩ, album) có hiệu ứng parallax theo [listState].
 */
@Composable
internal fun DetailHeroBackground(
    title: String,
    artworkUrl: String?,
    isArtist: Boolean,
    isFavorites: Boolean,
    listState: LazyListState
) {
    // Dải Gradient AMOLED kéo dài mượt mà từ startY = 240f đến endY = 1550f
    val bottomScrim = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                ObsidianBlack.copy(alpha = 0.40f),
                ObsidianBlack.copy(alpha = 0.85f),
                ObsidianBlack
            ),
            startY = 240f,
            endY = 1550f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        if (isFavorites) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ApexRose.copy(alpha = 0.35f), ObsidianBlack)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = ApexRose.copy(alpha = 0.45f),
                    modifier = Modifier
                        .padding(bottom = 120.dp)
                        .size(130.dp)
                        .graphicsLayer {
                            val offset = listState.firstVisibleItemScrollOffset
                            if (listState.firstVisibleItemIndex == 0) {
                                translationY = -offset * 0.45f
                                alpha = (1f - (offset / 900f)).coerceIn(0f, 1f)
                            }
                        }
                )
            }
        } else if (!artworkUrl.isNullOrBlank()) {
            // Ảnh bìa tràn viền 100% toàn chiều ngang màn hình (Full-Bleed FillWidth), hiển thị trọn vẹn không bị cắt 2 bên
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val offset = listState.firstVisibleItemScrollOffset
                        if (listState.firstVisibleItemIndex == 0) {
                            translationY = -offset * 0.45f
                            alpha = (1f - (offset / 900f)).coerceIn(0f, 1f)
                        }
                    }
            )
        } else if (isArtist) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = IvoryMuted,
                    modifier = Modifier
                        .padding(bottom = 120.dp)
                        .size(130.dp)
                        .graphicsLayer {
                            val offset = listState.firstVisibleItemScrollOffset
                            if (listState.firstVisibleItemIndex == 0) {
                                translationY = -offset * 0.45f
                                alpha = (1f - (offset / 900f)).coerceIn(0f, 1f)
                            }
                        }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = IvoryMuted,
                    modifier = Modifier
                        .padding(bottom = 120.dp)
                        .size(130.dp)
                        .graphicsLayer {
                            val offset = listState.firstVisibleItemScrollOffset
                            if (listState.firstVisibleItemIndex == 0) {
                                translationY = -offset * 0.45f
                                alpha = (1f - (offset / 900f)).coerceIn(0f, 1f)
                            }
                        }
                )
            }
        }

        // Lớp làm mềm chân ảnh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bottomScrim)
        )
    }
}

/** Phần đầu danh sách: tên, nghệ sĩ + Hi-Res, số bài & thời lượng, nút "Phát tất cả" + "Trộn bài". */
@Composable
internal fun DetailHeroHeader(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    showSnackbar: ShowSnackbar
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 300.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ObsidianBlack.copy(alpha = 0.85f),
                        ObsidianBlack,
                        ObsidianBlack
                    )
                )
            )
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tên Album
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = AppTheme.colors.textPrimary,
                fontSize = 26.sp,
                lineHeight = 32.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Tên Ca sĩ & Huy hiệu Hi-Res
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = IvoryHigh,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (tracks.any { it.isHiRes }) {
                Spacer(modifier = Modifier.width(8.dp))
                ApexHiResBadge()
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Metadata số bài & thời lượng
        Text(
            text = if (tracks.isNotEmpty()) "${tracks.size} bài hát • ${formatTotalDuration(tracks)}" else subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AppTheme.colors.textSecondary,
                fontSize = 13.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        // NÚT "PHÁT TẤT CẢ" + NÚT TRỘN BÀI
        Row(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(PillShape)
                    .background(AppTheme.colors.textPrimary)
                    .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                        if (tracks.isNotEmpty()) {
                            onPlayAll(tracks)
                        } else {
                            showSnackbar("Danh sách này chưa có bài hát nào", null, null)
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = AppTheme.colors.onInverse,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Phát tất cả",
                    color = AppTheme.colors.onInverse,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            }

            ApexCircularGlassButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_widget_shuffle),
                contentDescription = "Trộn bài",
                onClick = {
                    if (tracks.isNotEmpty()) {
                        onShuffleAll(tracks)
                    } else {
                        showSnackbar("Danh sách này chưa có bài hát nào", null, null)
                    }
                },
                size = 48.dp,
                iconSize = 22.dp,
                backgroundColor = AppTheme.colors.textPrimary,
                iconTint = AppTheme.colors.onInverse
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}
