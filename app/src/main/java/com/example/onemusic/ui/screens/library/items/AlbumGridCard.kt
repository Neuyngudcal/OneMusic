package com.example.onemusic.ui.screens.library.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SquircleLarge
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.theme.apexGroupedCardItem
import com.example.onemusic.ui.utils.apexBounceClick

/**
 * Vinyl Disc visual effect peeked behind or overlaid on album art.
 */
@Composable
fun VinylDiscEffect(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CharcoalBlack)
            .border(1.dp, IvoryStroke.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Outer groove ring
        Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .clip(CircleShape)
                .border(0.7.dp, PrimaryIvory.copy(alpha = 0.12f), CircleShape)
        )
        // Mid groove ring
        Box(
            modifier = Modifier
                .fillMaxSize(0.64f)
                .clip(CircleShape)
                .border(0.7.dp, PrimaryIvory.copy(alpha = 0.14f), CircleShape)
        )
        // Center label hole
        Box(
            modifier = Modifier
                .fillMaxSize(0.32f)
                .clip(CircleShape)
                .background(PrimaryIvory.copy(alpha = 0.25f))
                .border(1.dp, PrimaryIvory.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.28f)
                    .clip(CircleShape)
                    .background(CharcoalBlack)
            )
        }
    }
}

/**
 * Grid Card for Album with One UI Squircle shape and Vinyl disc styling.
 */
@Composable
fun AlbumGridCard(
    albumName: String,
    artistName: String,
    artworkUrl: String?,
    trackCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Column(
        modifier = modifier
            .then(
                if (isCompact) Modifier.apexGlassCard(shape = RoundedCornerShape(18.dp)).padding(8.dp)
                else Modifier
            )
            .apexBounceClick(
                scaleDown = if (isCompact) 0.96f else 0.95f,
                enableHaptic = true,
                onClick = onClick
            )
    ) {
        // Squircle Artwork Frame with subtle Vinyl peek
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            // Vinyl Record Disc peeking out slightly from the right-top
            VinylDiscEffect(
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .align(Alignment.TopEnd)
            )

            // Front Album Sleeve
            Box(
                modifier = Modifier
                    .fillMaxSize(0.96f)
                    .align(Alignment.BottomStart)
                    .clip(if (isCompact) RoundedCornerShape(14.dp) else SquircleLarge)
                    .background(SurfaceActiveIndicator)
                    .border(0.7.dp, IvoryStroke.copy(alpha = 0.5f), if (isCompact) RoundedCornerShape(14.dp) else SquircleLarge)
            ) {
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = albumName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = null,
                            tint = IvoryDisabled,
                            modifier = Modifier.size(if (isCompact) 36.dp else 52.dp)
                        )
                    }
                }

                // Track Count badge in bottom corner
                if (trackCount > 0 && !isCompact) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CharcoalBlack.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$trackCount bài",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PrimaryIvory,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

        // Album Name
        Text(
            text = albumName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = if (isCompact) 12.sp else 15.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(if (isCompact) 1.dp else 2.dp))

        // Artist Name
        Text(
            text = artistName,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = if (isCompact) 10.5.sp else 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * List Item Card for Album.
 */
@Composable
fun AlbumListItem(
    albumName: String,
    artistName: String,
    artworkUrl: String?,
    trackCount: Int,
    index: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .apexGroupedCardItem(index = index, total = total, cornerRadius = 26.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .apexBounceClick(
                        scaleDown = 0.98f,
                        enableHaptic = true,
                        onClick = onClick
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vinyl peek in List
                Box(modifier = Modifier.size(56.dp)) {
                    VinylDiscEffect(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.CenterEnd)
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceActiveIndicator)
                            .border(0.6.dp, IvoryStroke.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = artworkUrl,
                                contentDescription = albumName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = albumName,
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
                        text = "$artistName • $trackCount bài hát",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (index < total - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 86.dp, end = 16.dp),
                    thickness = 0.6.dp,
                    color = SurfaceDivider
                )
            }
        }
    }
}
