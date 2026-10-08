package com.example.onemusic.ui.screens.search

import com.example.onemusic.theme.AppTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import com.example.onemusic.data.search.LibraryGrouping
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.ui.components.ApexHiResBadge
import coil.compose.AsyncImage
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.search.MatchedAlbum
import com.example.onemusic.data.search.MatchedArtist
import com.example.onemusic.data.search.MatchedTrack
import com.example.onemusic.ui.utils.apexBounceClick
import AppTheme.colors.surfaceActiveIndicator
import com.example.onemusic.theme.apexFrostedGlass
import com.example.onemusic.theme.apexGlassCard

@Composable
internal fun SearchAlbumCard(
    album: MatchedAlbum,
    allTracks: List<Track>,
    onClick: () -> Unit,
    onPlayTracks: (List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    val albumTracks = remember(album, allTracks) {
        LibraryGrouping.tracksOfAlbum(allTracks, album.albumKey)
            .ifEmpty { listOf(album.representativeTrack) }
    }

    Column(
        modifier = modifier
            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true, onClick = onClick)
    ) {
        // Artwork Frame (1:1 Square)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(AppTheme.colors.surfaceActiveIndicator)
        ) {
            val artUrl = album.representativeTrack.artworkUrl
            if (!artUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = album.albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.surface1),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = AppTheme.colors.disabled,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Album Name
        Text(
            text = album.albumName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                fontSize = 15.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Artist Name
        Text(
            text = album.artistName,
            style = MaterialTheme.typography.bodySmall.copy(
                color = AppTheme.colors.textSecondary,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ArtistRow(
    artistName: String,
    trackCount: Int,
    artistImageUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!artistImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = artistImageUrl,
                contentDescription = artistName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .border(0.7.dp, AppTheme.colors.stroke, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.surfaceActiveIndicator),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = AppTheme.colors.textPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artistName,
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
                text = "$trackCount bài hát",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun ArtistCard(
    artist: MatchedArtist,
    artistImageUrl: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .apexGlassCard(shape = RoundedCornerShape(20.dp))
            .apexBounceClick(scaleDown = 0.94f, enableHaptic = true, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Avatar with Photo or One UI Mic Icon
            if (!artistImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artistImageUrl,
                    contentDescription = artist.artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(0.7.dp, AppTheme.colors.stroke, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.surfaceActiveIndicator),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = AppTheme.colors.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = artist.artistName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${artist.trackCount} bài hát",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun TrackResultRow(
    matchedTrack: MatchedTrack,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val track = matchedTrack.track
    val context = androidx.compose.ui.platform.LocalContext.current
    val isHiResBadgeEnabled = remember(context) {
        com.example.onemusic.data.local.SettingsPreferences(context).getSettings().isHiResBadgeEnabled
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .apexBounceClick(
                scaleDown = 0.98f,
                enableHaptic = true,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // If matched via lyrics snippet, show subtle quotation badge
            if (!matchedTrack.matchedLyricSnippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .apexFrostedGlass()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatQuote,
                        contentDescription = null,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "\"${matchedTrack.matchedLyricSnippet}\"",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AppTheme.colors.high,
                            fontStyle = FontStyle.Italic,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Hi-Res Badge
        if (track.isHiRes && isHiResBadgeEnabled) {
            Spacer(modifier = Modifier.width(8.dp))
            ApexHiResBadge()
        }

        // Favorite Heart Button
        if (onToggleFavorite != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                    .size(36.dp)
                    .clip(CircleShape)
                    .apexBounceClick(scaleDown = 0.85f, enableHaptic = true, onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (track.isFavorite) "Bỏ yêu thích" else "Yêu thích",
                    tint = if (track.isFavorite) AppTheme.colors.danger else AppTheme.colors.faint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
