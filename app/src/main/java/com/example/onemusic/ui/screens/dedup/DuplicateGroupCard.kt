package com.example.onemusic.ui.screens.dedup

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.onemusic.data.dedup.DuplicateAudioDetector
import com.example.onemusic.data.dedup.DuplicateGroup
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.AvatarGreen
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.IvoryHairline
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.ui.utils.formatDuration

@Composable
internal fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedTrackIds: Map<String, Boolean>,
    currentPlayingTrackId: String?,
    isPlaying: Boolean,
    onToggleTrack: (String) -> Unit,
    onPlayPreview: (Track) -> Unit
) {
    val context = LocalContext.current
    val primary = group.primaryTrack

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .apexGlassCard(shape = RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Header
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = group.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PRIMARY TRACK (Khuyên giữ lại)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AvatarGreen.copy(alpha = 0.14f))
                    .border(0.6.dp, AvatarGreen.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = primary.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AvatarGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GIỮ LẠI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = primary.bitRate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AvatarGreen,
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val primarySize = DuplicateAudioDetector.getTrackFileSize(context, primary)
                    Text(
                        text = "${formatDuration(primary.durationMs, padMinutes = true)} • ${Formatter.formatFileSize(context, primarySize)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    )
                }

                // Play preview button
                val isCurrent = currentPlayingTrackId == primary.id
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) Brand else IvorySubtle)
                        .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                            onPlayPreview(primary)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isCurrent && isPlaying) "Tạm dừng nghe thử" else "Nghe thử",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DUPLICATE TRACKS (Đề xuất lọc)
            group.duplicateTracks.forEachIndexed { index, dupTrack ->
                val isSelected = selectedTrackIds[dupTrack.id] ?: false
                val isCurrent = currentPlayingTrackId == dupTrack.id
                val dupSize = DuplicateAudioDetector.getTrackFileSize(context, dupTrack)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) ApexRose.copy(alpha = 0.15f) else SurfaceCard)
                        .border(
                            0.5.dp,
                            if (isSelected) ApexRose.copy(alpha = 0.4f) else IvoryHairline,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggleTrack(dupTrack.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ApexRose else IvoryStroke),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceActiveIndicator)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "TRÙNG LẶP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        fontSize = 8.5.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dupTrack.bitRate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = IvoryHigh,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${formatDuration(dupTrack.durationMs, padMinutes = true)} • ${Formatter.formatFileSize(context, dupSize)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Play preview
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent) Brand else IvorySubtle)
                            .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                onPlayPreview(dupTrack)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isCurrent && isPlaying) "Tạm dừng nghe thử" else "Nghe thử",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
