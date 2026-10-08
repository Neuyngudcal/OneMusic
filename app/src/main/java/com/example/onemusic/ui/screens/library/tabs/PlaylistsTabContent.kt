package com.example.onemusic.ui.screens.library.tabs

import com.example.onemusic.theme.AppTheme
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
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
import com.example.onemusic.theme.PillShape
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
    onOpenNewPlaylistDialog: () -> Unit,
    onExportPlaylistM3u: ((CustomPlaylist) -> Unit)?,
    onDeletePlaylist: ((CustomPlaylist) -> Unit)?
) {
    // Rows in the grouped card: "Create" + Favorites + custom playlists
    val totalRows = 2 + customPlaylists.size

    // 1. Create Playlist row (first row of the grouped card)
    item(key = "playlists_create_row") {
        CreatePlaylistRow(total = totalRows, onClick = onOpenNewPlaylistDialog)
    }

    // 2. Fixed Favorite Playlist item
    item(key = "favorite_playlist_item") {
        val favoriteTracks = tracks.filter { it.isFavorite }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .apexGroupedCardItem(index = 1, total = totalRows, cornerRadius = 26.dp)
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
                            .background(AppTheme.colors.surfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Yêu thích",
                            tint = AppTheme.colors.danger,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bài hát yêu thích",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${favoriteTracks.size} bài hát",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (totalRows > 2) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = AppTheme.colors.divider
                    )
                }
            }
        }
    }

    // 3. Custom Playlists
    itemsIndexed(
        items = customPlaylists,
        key = { index, pl -> "custom_pl_${pl.id}_$index" }
    ) { idx, pl ->
        val currentIndex = idx + 2

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .apexGroupedCardItem(index = currentIndex, total = totalRows, cornerRadius = 26.dp)
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
                            .background(AppTheme.colors.surfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = pl.name,
                            tint = AppTheme.colors.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pl.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${pl.trackIds.size} bài hát",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppTheme.colors.textSecondary,
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
                            iconTint = AppTheme.colors.textSecondary
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
                            iconTint = AppTheme.colors.danger
                        )
                    }
                }

                if (currentIndex < totalRows - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = AppTheme.colors.divider
                    )
                }
            }
        }
    }
}

/**
 * Dialog for creating a new playlist.
 */
/**
 * "Tạo playlist mới" styled like a playlist row, so the tab has no separate button bar.
 */
@Composable
private fun CreatePlaylistRow(total: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .apexGroupedCardItem(index = 0, total = total, cornerRadius = 26.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .apexBounceClick(scaleDown = 0.98f, enableHaptic = true, onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.colors.surfaceActiveIndicator),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = AppTheme.colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Tạo playlist mới",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Favorites row always follows, so the divider is unconditional
            HorizontalDivider(
                modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                thickness = 0.6.dp,
                color = AppTheme.colors.divider
            )
        }
    }
}

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
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = newPlaylistName,
                onValueChange = { newPlaylistName = it },
                placeholder = { Text("Tên danh sách phát...", color = AppTheme.colors.textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.accent,
                    unfocusedBorderColor = AppTheme.colors.divider,
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary
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
                        .background(AppTheme.colors.surfaceControl)
                        .border(1.5.dp, AppTheme.colors.borderStrong, PillShape)
                        .apexBounceClick(scaleDown = 0.92f) { onDismissRequest() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Hủy", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(AppTheme.colors.textPrimary)
                        .apexBounceClick(scaleDown = 0.92f) {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylist(newPlaylistName.trim())
                            }
                            onDismissRequest()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Tạo", color = AppTheme.colors.onInverse, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
