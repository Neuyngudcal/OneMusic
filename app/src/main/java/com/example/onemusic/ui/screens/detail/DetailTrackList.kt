package com.example.onemusic.ui.screens.detail

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.apexFrostedGlass
import com.example.onemusic.theme.apexGroupedCardItem
import com.example.onemusic.ui.components.ApexHiResBadge
import com.example.onemusic.ui.utils.ShowSnackbar
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

/** Một bài trong danh sách (thẻ nhóm): số thứ tự, ảnh, tên/nghệ sĩ, nút tim, nút xóa khỏi playlist. */
@Composable
internal fun DetailTrackRow(
    index: Int,
    total: Int,
    track: Track,
    tracks: List<Track>,
    isCurrent: Boolean,
    isHiResBadgeEnabled: Boolean,
    hazeState: HazeState,
    customPlaylist: CustomPlaylist?,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onTrackLongClick: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRemoveTrackFromPlaylist: ((playlistId: String, trackId: String) -> Unit)?,
    onAddTrackToPlaylist: ((playlistId: String, trackId: String) -> Unit)?,
    showSnackbar: ShowSnackbar
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianBlack)
            .padding(horizontal = 20.dp)
            .apexGroupedCardItem(index = index, total = total)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .apexBounceClick(
                        scaleDown = 0.98f,
                        enableHaptic = true,
                        onLongClick = onTrackLongClick
                    ) {
                        onTrackSelect(track, tracks)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isCurrent) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.width(28.dp)
                )

                if (track.artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.colors.surfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = AppTheme.colors.accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) AppTheme.colors.accent else AppTheme.colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isHiResBadgeEnabled && track.isHiRes) {
                            ApexHiResBadge()
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                        .size(36.dp)
                        .clip(CircleShape)
                        .apexFrostedGlass(
                            backgroundColor = AppTheme.colors.surface1.copy(alpha = 0.60f),
                            blurRadius = 12.dp,
                            hazeState = hazeState
                        )
                        .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                            onToggleFavorite(track.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (track.isFavorite) "Bỏ yêu thích" else "Yêu thích",
                        tint = if (track.isFavorite) ApexRose else AppTheme.colors.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Xóa khỏi playlist (nếu custom playlist)
                if (customPlaylist != null && onRemoveTrackFromPlaylist != null) {
                    // Khoảng cách rộng hơn để giảm bấm nhầm giữa nút tim và nút xóa
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                            .size(36.dp)
                            .clip(CircleShape)
                            .apexFrostedGlass(
                                backgroundColor = AppTheme.colors.surface1.copy(alpha = 0.60f),
                                blurRadius = 12.dp,
                                hazeState = hazeState
                            )
                            .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                onRemoveTrackFromPlaylist(customPlaylist.id, track.id)
                                // Thao tác nhẹ → làm ngay + cho "Hoàn tác" (bài sẽ quay về CUỐI playlist)
                                showSnackbar(
                                    "Đã xóa \"${track.title}\" khỏi \"${customPlaylist.name}\"",
                                    if (onAddTrackToPlaylist != null) "Hoàn tác" else null
                                ) {
                                    onAddTrackToPlaylist?.invoke(customPlaylist.id, track.id)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Xóa khỏi playlist",
                            tint = ApexRose.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (index < tracks.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 94.dp, end = 16.dp),
                    thickness = 0.6.dp,
                    color = AppTheme.colors.divider
                )
            }
        }
    }
}
