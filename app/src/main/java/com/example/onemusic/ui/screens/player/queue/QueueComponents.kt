package com.example.onemusic.ui.screens.player.queue

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.R
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.RepeatMode
import com.example.onemusic.theme.ActivePillBg
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryMedium
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ScrimColor
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.screens.player.controls.ApexDynamicEqualizerBars
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.ui.utils.formatDuration
import dev.chrisbanes.haze.HazeState

@Composable
fun QueueTopHeader(
    track: Track?,
    isPlaying: Boolean,
    onToggleFavorite: (String) -> Unit = {},
    onAddToPlaylist: ((Track) -> Unit)? = null,
    isShuffle: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    repeatMode: RepeatMode = RepeatMode.OFF,
    onCycleRepeat: () -> Unit = {},
    playbackSpeed: Float = 1.0f,
    onOpenSpeedMenu: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenDetails: () -> Unit = {},
    onOpenOptions: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    if (track == null) return
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Album Artwork (46dp x 46dp, bo góc 12dp)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(track.artworkUrl)
                        .crossfade(true)
                        .size(128, 128)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = IvoryFaint,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScrimColor),
                    contentAlignment = Alignment.Center
                ) {
                    ApexDynamicEqualizerBars(
                        isPlaying = true,
                        barColor = PrimaryIvory
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Track Title & Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = IvoryMedium,
                    fontSize = 13.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QueuePlaybackModesRow(
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isAutoplay: Boolean,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Shuffle Mode Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(PillShape)
                .background(if (isShuffle) ActivePillBg else IvorySubtle)
                .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                    onToggleShuffle()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_widget_shuffle),
                contentDescription = "Trộn bài",
                tint = PrimaryIvory,
                modifier = Modifier.size(20.dp)
            )
        }

        // Repeat Mode Pill
        val isRepeatActive = repeatMode != RepeatMode.OFF
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(PillShape)
                .background(if (isRepeatActive) ActivePillBg else IvorySubtle)
                .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                    onCycleRepeat()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Lặp lại",
                tint = PrimaryIvory,
                modifier = Modifier.size(20.dp)
            )
        }

        // Autoplay Infinity Pill (∞)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(PillShape)
                .background(if (isAutoplay) ActivePillBg else IvorySubtle)
                .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                    onToggleAutoplay()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AllInclusive,
                contentDescription = "Tự động phát",
                tint = PrimaryIvory,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun QueueFlatTrackRow(
    track: Track,
    showReorder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reorderContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                onClick()
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Artwork (42dp x 42dp, bo góc 10dp)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(track.artworkUrl)
                        .crossfade(true)
                        .size(128, 128)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = IvoryFaint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = IvoryMedium,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelSmall.copy(
                color = IvoryFaint,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal
            )
        )

        // Reorder handle if requested
        if (showReorder && reorderContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            reorderContent()
        }
    }
}
