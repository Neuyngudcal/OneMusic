package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.utils.apexBounceClick

@Composable
fun AddToPlaylistMultipleDialog(
    tracks: List<Track>,
    playlists: List<CustomPlaylist>,
    onCreatePlaylist: (String) -> CustomPlaylist?,
    onAddToPlaylist: (playlistId: String, trackId: String) -> Boolean,
    // Chỉ gọi khi đã thêm thành công (khác onDismiss: đóng dialog, kể cả khi bấm Hủy/ra ngoài)
    onAdded: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    ApexDialogContainer(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
                AddToPlaylistTitle()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đang chọn ${tracks.size} bài hát",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AppTheme.colors.accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                )
                Spacer(modifier = Modifier.height(18.dp))

                if (isCreatingNew) {
                    NewPlaylistNameField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(AppTheme.colors.surfaceControl)
                                .border(1.5.dp, AppTheme.colors.borderStrong, PillShape)
                                .apexBounceClick(scaleDown = 0.92f) { isCreatingNew = false }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Hủy",
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(AppTheme.colors.textPrimary)
                                .apexBounceClick(scaleDown = 0.92f) {
                                    if (newPlaylistName.isNotBlank()) {
                                        val pl = onCreatePlaylist(newPlaylistName.trim())
                                        if (pl != null) {
                                            var addedCount = 0
                                            tracks.forEach { tr ->
                                                if (onAddToPlaylist(pl.id, tr.id)) addedCount++
                                            }
                                            Toast.makeText(context, "Đã thêm $addedCount bài vào ${pl.name}", Toast.LENGTH_SHORT).show()
                                            onAdded()
                                            onDismiss()
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Tạo & Thêm",
                                color = AppTheme.colors.onInverse,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.colors.surfaceActiveIndicator)
                            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) { isCreatingNew = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppTheme.colors.textPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Tạo mới",
                                tint = AppTheme.colors.onInverse,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tạo danh sách phát mới",
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "Chưa có danh sách phát nào",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                            var count = 0
                                            tracks.forEach { tr ->
                                                if (onAddToPlaylist(pl.id, tr.id)) count++
                                            }
                                            Toast.makeText(context, "Đã thêm $count bài vào ${pl.name}", Toast.LENGTH_SHORT).show()
                                            onAdded()
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = AppTheme.colors.medium,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = AppTheme.colors.textPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pl.trackIds.size} bài hát",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = AppTheme.colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = AppTheme.colors.divider)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AddToPlaylistCloseButton(onDismiss = onDismiss)
            }
        }
}
