package com.example.onemusic.ui.screens.library.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGroupedCardItem
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexDialogContainer
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

/**
 * Extension for rendering Playlists tab content inside LibraryScreen's LazyList.
 */
fun LazyListScope.playlistsTabContent(
    customPlaylists: List<CustomPlaylist>,
    tracks: List<Track>,
    onOpenFavoritePlaylist: () -> Unit,
    onOpenPlaylistDetail: (CustomPlaylist) -> Unit,
    onPlayTracks: (List<Track>) -> Unit,
    onOpenNewPlaylistDialog: () -> Unit,
    onImportPlaylistM3u: (() -> Unit)?,
    onExportPlaylistM3u: ((CustomPlaylist) -> Unit)?,
    onDeletePlaylist: ((CustomPlaylist) -> Unit)?
) {
    val totalPlaylists = 1 + customPlaylists.size

    // 1. Quick Actions Row: Create Playlist & Import M3U
    item(key = "playlists_quick_actions") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Create Playlist Pill Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(PillShape)
                    .background(PrimaryIvory)
                    .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                        onOpenNewPlaylistDialog()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Tạo playlist",
                        tint = CharcoalBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Tạo playlist mới",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalBlack,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // Import M3U Button (if available)
            if (onImportPlaylistM3u != null) {
                ApexCircularGlassButton(
                    icon = Icons.Rounded.UploadFile,
                    contentDescription = "Nhập playlist .m3u8",
                    onClick = onImportPlaylistM3u,
                    size = 46.dp,
                    iconSize = 22.dp,
                    backgroundColor = SurfaceControl,
                    iconTint = PrimaryIvory
                )
            }
        }
    }

    // 2. Section Label
    item(key = "playlists_section_label") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DANH SÁCH PHÁT ($totalPlaylists)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                )
            )
        }
    }

    // 3. Fixed Favorite Playlist item
    item(key = "favorite_playlist_item") {
        val favoriteTracks = tracks.filter { it.isFavorite }
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
                        .apexBounceClick(
                            scaleDown = 0.98f,
                            enableHaptic = true,
                            onClick = onOpenFavoritePlaylist
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Yêu thích",
                            tint = ApexRose,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bài hát yêu thích",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${favoriteTracks.size} bài hát",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    ApexCircularGlassButton(
                        icon = Icons.Rounded.PlayArrow,
                        contentDescription = "Phát bài yêu thích",
                        onClick = {
                            if (favoriteTracks.isNotEmpty()) {
                                onPlayTracks(favoriteTracks)
                            }
                        },
                        size = 38.dp,
                        iconSize = 20.dp,
                        iconTint = PrimaryIvory
                    )
                }

                if (totalPlaylists > 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = SurfaceDivider
                    )
                }
            }
        }
    }

    // 4. Custom Playlists
    itemsIndexed(
        items = customPlaylists,
        key = { index, pl -> "custom_pl_${pl.id}_$index" }
    ) { idx, pl ->
        val currentIndex = idx + 1
        val plTracks = tracks.filter { it.id in pl.trackIds }

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
                        .apexBounceClick(
                            scaleDown = 0.98f,
                            enableHaptic = true,
                            onClick = { onOpenPlaylistDetail(pl) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = pl.name,
                            tint = PrimaryIvory,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pl.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${pl.trackIds.size} bài hát",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onExportPlaylistM3u != null) {
                        ApexCircularGlassButton(
                            icon = Icons.Rounded.UploadFile,
                            contentDescription = "Xuất playlist",
                            onClick = { onExportPlaylistM3u(pl) },
                            size = 36.dp,
                            iconSize = 18.dp,
                            iconTint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (onDeletePlaylist != null) {
                        ApexCircularGlassButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = "Xóa playlist",
                            onClick = { onDeletePlaylist(pl) },
                            size = 36.dp,
                            iconSize = 18.dp,
                            iconTint = ApexRose
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    ApexCircularGlassButton(
                        icon = Icons.Rounded.PlayArrow,
                        contentDescription = "Phát playlist",
                        onClick = {
                            if (plTracks.isNotEmpty()) {
                                onPlayTracks(plTracks)
                            }
                        },
                        size = 38.dp,
                        iconSize = 20.dp,
                        iconTint = PrimaryIvory
                    )
                }

                if (currentIndex < totalPlaylists - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = SurfaceDivider
                    )
                }
            }
        }
    }
}

/**
 * Dialog for creating a new playlist.
 */
@Composable
fun LibraryNewPlaylistDialog(
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }

    ApexDialogContainer(
        onDismissRequest = onDismissRequest,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(
                text = "Tạo Danh Sách Phát Mới",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = newPlaylistName,
                onValueChange = { newPlaylistName = it },
                placeholder = { Text("Tên danh sách phát...", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand,
                    unfocusedBorderColor = SurfaceDivider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(SurfaceControl)
                        .border(1.5.dp, SurfaceBorderStrong, PillShape)
                        .apexBounceClick(scaleDown = 0.92f) { onDismissRequest() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Hủy", color = PrimaryIvory, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(PrimaryIvory)
                        .apexBounceClick(scaleDown = 0.92f) {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylist(newPlaylistName.trim())
                            }
                            onDismissRequest()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Tạo", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
