package com.example.onemusic.ui.screens.library.tabs

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.theme.apexGroupedCardItem
import com.example.onemusic.theme.avatarColorFor
import com.example.onemusic.ui.utils.apexBounceClick

/**
 * Extension for rendering Artists tab content inside LibraryScreen's LazyList.
 */
fun LazyListScope.artistsTabContent(
    artists: List<Pair<String, List<Track>>>,
    artistImages: Map<String, String>,
    getCachedArtistImageUrl: (String) -> String?,
    onArtistClick: (String) -> Unit
) {
    // 1. Empty State or Artists List
    if (artists.isEmpty()) {
        item(key = "empty_artists") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .apexGlassCard(shape = RoundedCornerShape(26.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có nghệ sĩ nào",
                    style = MaterialTheme.typography.bodyMedium.copy(color = AppTheme.colors.textSecondary)
                )
            }
        }
    } else {
        itemsIndexed(
            items = artists,
            key = { index, (artistName, _) -> "artist_${artistName}_$index" }
        ) { index, (artistName, artistTracks) ->
            val total = artists.size
            val initials = artistName.split(" ").filter { it.isNotBlank() }.take(2)
                .map { it.first().uppercase() }.joinToString("")
            val avatarColor = remember(artistName) { avatarColorFor(artistName) }
            val artistImageUrl = artistImages[artistName.trim().lowercase()] ?: getCachedArtistImageUrl(artistName)

            Box(
                modifier = Modifier
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
                                onClick = { onArtistClick(artistName) }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Soft Round Avatar with image or initials
                        if (!artistImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = artistImageUrl,
                                contentDescription = artistName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(0.7.dp, AppTheme.colors.stroke, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (initials.isNotBlank()) initials else "♪",
                                    color = AppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artistName,
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
                                text = "${artistTracks.size} bài hát",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (index < total - 1) {
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
}
