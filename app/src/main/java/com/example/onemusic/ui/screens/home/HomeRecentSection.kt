package com.example.onemusic.ui.screens.home

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard
import androidx.compose.foundation.lazy.LazyListScope

/** "Nghe gần đây" + "Có thể bạn sẽ thích" (chỉ gọi khi thư viện có bài). */
internal fun LazyListScope.homeRecentSection(
    recentlyPlayedTracks: List<Track>,
    suggestedTracks: List<Track>,
    currentTrackId: String?,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onTrackLongClick: (Track) -> Unit
) {
    // 3. Section: "NGHE GẦN ĐÂY" (Recently Played)
    item(key = "section_recent_title") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NGHE GẦN ĐÂY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                )
            )
        }
    }

    if (recentlyPlayedTracks.isEmpty()) {
        item(key = "empty_recents") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .apexGlassCard(shape = RoundedCornerShape(26.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có bài hát nào được phát gần đây",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                )
            }
        }
    } else {
        item(key = "recent_carousel") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(recentlyPlayedTracks, key = { "recent_" + it.id }) { track ->
                    val isCurrent = track.id == currentTrackId
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .apexBounceClick(
                                scaleDown = 0.95f,
                                enableHaptic = true,
                                onClick = { onTrackSelect(track, recentlyPlayedTracks) },
                                onLongClick = { onTrackLongClick(track) }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceActiveIndicator)
                        ) {
                            if (!track.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = IvoryDisabled,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) Brand else PrimaryIvory,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // 3.5 Section: CÓ THỂ BẠN SẼ THÍCH
    if (suggestedTracks.isNotEmpty()) {
        item(key = "section_suggested_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CÓ THỂ BẠN SẼ THÍCH",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    )
                )
            }
        }

        item(key = "suggested_carousel") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(suggestedTracks, key = { "suggested_" + it.id }) { track ->
                    val isCurrent = track.id == currentTrackId
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .apexBounceClick(
                                scaleDown = 0.95f,
                                enableHaptic = true,
                                onClick = { onTrackSelect(track, suggestedTracks) }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceActiveIndicator)
                        ) {
                            if (!track.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = IvoryDisabled,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) Brand else PrimaryIvory,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
