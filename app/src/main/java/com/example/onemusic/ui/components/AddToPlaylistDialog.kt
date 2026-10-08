package com.example.onemusic.ui.components

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
import androidx.compose.material.icons.rounded.Check
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
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.ui.utils.apexBounceClick

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<CustomPlaylist>,
    onCreatePlaylist: (String) -> CustomPlaylist?,
    onAddToPlaylist: (playlistId: String, trackId: String) -> Boolean,
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
                    text = "${track.title} • ${track.artist}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(18.dp))

                if (isCreatingNew) {
                    NewPlaylistNameField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(SurfaceControl)
                                .border(1.5.dp, SurfaceBorderStrong, PillShape)
                                .apexBounceClick(scaleDown = 0.92f) { isCreatingNew = false }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
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
                                        val created = onCreatePlaylist(newPlaylistName.trim())
                                        if (created != null) {
                                            onAddToPlaylist(created.id, track.id)
                                            Toast.makeText(context, "Đã tạo và thêm vào \"${created.name}\"", Toast.LENGTH_SHORT).show()
                                        }
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Tạo & Thêm", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Create New Playlist Trigger Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceActiveIndicator)
                            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                                isCreatingNew = true
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryIvory),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Tạo playlist",
                                tint = CharcoalBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Tạo danh sách phát mới",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIvory,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = "Chưa có danh sách phát nào.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary
                            ),
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(playlists, key = { it.id }) { pl ->
                                val containsTrack = pl.trackIds.contains(track.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                            if (containsTrack) {
                                                Toast.makeText(context, "Bài hát đã có trong \"${pl.name}\"", Toast.LENGTH_SHORT).show()
                                            } else {
                                                onAddToPlaylist(pl.id, track.id)
                                                Toast.makeText(context, "Đã thêm vào \"${pl.name}\"", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceActiveIndicator),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                            contentDescription = null,
                                            tint = if (containsTrack) Brand else TextPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pl.trackIds.size} bài hát",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary
                                            )
                                        )
                                    }
                                    if (containsTrack) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Đã có",
                                            tint = Brand,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = SurfaceDivider
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AddToPlaylistCloseButton(onDismiss = onDismiss)
            }
        }
}
