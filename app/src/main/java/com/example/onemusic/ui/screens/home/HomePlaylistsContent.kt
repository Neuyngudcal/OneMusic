package com.example.onemusic.ui.screens.home

import com.example.onemusic.theme.AppTheme
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.apexGroupedCardItem

/** HomeSubView.PLAYLISTS: danh sách playlist (Yêu thích + tự tạo), nhập/xuất .m3u8, tạo/xóa playlist. */
@Composable
internal fun HomePlaylistsContent(
    tracks: List<Track>,
    customPlaylists: List<CustomPlaylist>,
    onBack: () -> Unit,
    onImportPlaylistM3u: (() -> Unit)?,
    onExportPlaylistM3u: ((CustomPlaylist) -> Unit)?,
    onCreatePlaylistClick: () -> Unit,
    onRequestDeletePlaylist: (CustomPlaylist) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylist: (CustomPlaylist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
    ) {
        item(key = "playlists_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ApexCircularGlassButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Quay lại",
                    onClick = onBack,
                    size = 44.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 22.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (onImportPlaylistM3u != null) {
                    ApexCircularGlassButton(
                        icon = Icons.Rounded.FolderOpen,
                        contentDescription = "Nhập playlist .m3u8",
                        onClick = { onImportPlaylistM3u() },
                        size = 44.dp,
                        iconSize = 22.dp,
                        iconTint = AppTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                ApexCircularGlassButton(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Tạo playlist",
                    onClick = onCreatePlaylistClick,
                    size = 44.dp,
                    iconSize = 22.dp,
                    iconTint = AppTheme.colors.textPrimary
                )
            }
        }

        // Favorite Playlist & Custom Playlists in One UI 8.5 Grouped Card
        val favCount = tracks.count { it.isFavorite }
        val totalPlaylists = 1 + customPlaylists.size

        item(key = "favorite_playlist_item") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .apexGroupedCardItem(index = 0, total = totalPlaylists, cornerRadius = 26.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                onOpenFavorites()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppTheme.colors.danger),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = AppTheme.colors.onAccent,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bài hát yêu thích",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textPrimary,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$favCount bài hát",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                    if (totalPlaylists > 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 98.dp, end = 16.dp),
                            thickness = 0.6.dp,
                            color = AppTheme.colors.divider
                        )
                    }
                }
            }
        }

        // Custom Playlists
        itemsIndexed(customPlaylists, key = { index, pl -> "${pl.id}_$index" }) { idx, pl ->
            val currentIndex = 1 + idx
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .apexGroupedCardItem(index = currentIndex, total = totalPlaylists, cornerRadius = 26.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                onOpenPlaylist(pl)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppTheme.colors.surfaceActiveIndicator),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textPrimary,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${pl.trackIds.size} bài hát",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 13.5.sp
                                )
                            )
                        }

                        if (onExportPlaylistM3u != null) {
                            ApexCircularGlassButton(
                                icon = Icons.Rounded.Share,
                                contentDescription = "Xuất playlist .m3u8",
                                onClick = {
                                    onExportPlaylistM3u(pl)
                                },
                                size = 38.dp,
                                iconSize = 18.dp,
                                iconTint = AppTheme.colors.textPrimary,
                                backgroundColor = AppTheme.colors.surfaceActiveIndicator.copy(alpha = 0.60f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        ApexCircularGlassButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = "Xóa playlist",
                            // Chỉ mở hộp xác nhận, chưa xóa ngay
                            onClick = { onRequestDeletePlaylist(pl) },
                            size = 38.dp,
                            iconSize = 18.dp,
                            iconTint = AppTheme.colors.danger.copy(alpha = 0.85f),
                            backgroundColor = AppTheme.colors.surfaceActiveIndicator.copy(alpha = 0.60f)
                        )
                    }

                    if (currentIndex < totalPlaylists - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 98.dp, end = 16.dp),
                            thickness = 0.6.dp,
                            color = AppTheme.colors.divider
                        )
                    }
                }
            }
        }
    }
}
