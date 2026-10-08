package com.example.onemusic.ui.screens.search

import com.example.onemusic.theme.AppTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.RecentSearchEntry
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.ArtistImageRepository
import com.example.onemusic.data.search.MatchedAlbum
import com.example.onemusic.data.search.MatchedArtist
import com.example.onemusic.data.search.MatchedTrack
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.apexGroupedCardItem
import androidx.compose.foundation.lazy.LazyListScope

/** Nội dung khi ô tìm kiếm trống, theo tab: lịch sử tìm kiếm (Tất cả) hoặc toàn bộ nghệ sĩ / bài hát / album. */
internal fun LazyListScope.searchBlankState(
    selectedFilter: SearchFilterTab,
    recentSearches: List<RecentSearchEntry>,
    allArtists: List<MatchedArtist>,
    allAlbums: List<MatchedAlbum>,
    displayTracks: List<MatchedTrack>,
    displayTrackList: List<Track>,
    tracks: List<Track>,
    artistImages: Map<String, String>,
    artistImageRepository: ArtistImageRepository,
    onSearchQueryChange: (String) -> Unit,
    onClearAllRecentSearches: () -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onRecordRecentSearch: (query: String, label: String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onPlayTracks: (List<Track>) -> Unit
) {
    when (selectedFilter) {
        SearchFilterTab.ALL -> {
            // Recent Searches History
            if (recentSearches.isNotEmpty()) {
                item(key = "recent_searches_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đã tìm kiếm gần đây",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Xóa tất cả",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = AppTheme.colors.danger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier
                                .apexBounceClick(scaleDown = 0.90f) {
                                    onClearAllRecentSearches()
                                }
                        )
                    }
                }

                val totalRecents = recentSearches.size
                itemsIndexed(recentSearches, key = { index, it -> "${it.query}_$index" }) { index, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .apexGroupedCardItem(index = index, total = totalRecents, cornerRadius = 26.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                        onSearchQueryChange(item.query)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(AppTheme.colors.surfaceActiveIndicator),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.query,
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
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = AppTheme.colors.textSecondary,
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1
                                    )
                                }

                                ApexCircularGlassButton(
                                    icon = Icons.Rounded.Close,
                                    contentDescription = "Xóa khỏi lịch sử",
                                    onClick = {
                                        onRemoveRecentSearch(item.query)
                                    },
                                    size = 34.dp,
                                    iconSize = 16.dp,
                                    modifier = Modifier.minimumInteractiveComponentSize(), // vùng chạm ≥ 48dp
                                    iconTint = AppTheme.colors.textSecondary.copy(alpha = 0.85f),
                                    backgroundColor = AppTheme.colors.surfaceActiveIndicator.copy(alpha = 0.60f)
                                )
                            }

                            if (index < totalRecents - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                    thickness = 0.6.dp,
                                    color = AppTheme.colors.divider
                                )
                            }
                        }
                    }
                }
            } else {
                item(key = "empty_recents") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = AppTheme.colors.surfaceActiveIndicator,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Tìm kiếm bài hát, nghệ sĩ hoặc lời bài hát",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 15.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        SearchFilterTab.ARTISTS -> {
            // Display all artists matching HomeScreen in Grouped Card
            val totalArtists = allArtists.size
            itemsIndexed(allArtists, key = { index, it -> "${it.artistName}_$index" }) { index, artist ->
                val artistImageUrl = artistImages[artist.artistName.trim().lowercase()]
                    ?: artistImageRepository.getCachedImageUrl(artist.artistName)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .apexGroupedCardItem(index = index, total = totalArtists, cornerRadius = 26.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ArtistRow(
                            artistName = artist.artistName,
                            trackCount = artist.trackCount,
                            artistImageUrl = artistImageUrl,
                            onClick = {
                                onOpenArtist(artist.artistName)
                            }
                        )

                        if (index < totalArtists - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 98.dp, end = 16.dp),
                                thickness = 0.6.dp,
                                color = AppTheme.colors.divider
                            )
                        }
                    }
                }
            }
        }
        SearchFilterTab.SONGS -> {
            // Display all tracks with full LazyColumn virtualization
            val total = displayTracks.size
            itemsIndexed(
                items = displayTracks,
                key = { index, item -> "blank_track_${item.track.id}_$index" },
                contentType = { _, _ -> "track_item" }
            ) { index, matchedItem ->

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .apexGroupedCardItem(index = index, total = total, cornerRadius = 26.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TrackResultRow(
                            matchedTrack = matchedItem,
                            onClick = { onTrackSelect(matchedItem.track, displayTrackList) },
                            onToggleFavorite = { onToggleFavorite(matchedItem.track.id) },
                            onLongClick = { onTrackLongClick(matchedItem.track) }
                        )
                        if (index < total - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 102.dp, end = 16.dp),
                                thickness = 0.6.dp,
                                color = AppTheme.colors.divider
                            )
                        }
                    }
                }
            }
        }
        SearchFilterTab.ALBUMS -> {
            // Display all albums in authentic 2-column grid matching Home screen
            val pairs = allAlbums.chunked(2)
            items(pairs, key = { pair -> "blank_album_pair_" + pair.joinToString("_") { it.albumKey } }) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (album in pair) {
                        SearchAlbumCard(
                            album = album,
                            allTracks = tracks,
                            onClick = {
                                onRecordRecentSearch(album.albumName, "Album")
                                onOpenAlbum(album.albumKey)
                            },
                            onPlayTracks = onPlayTracks,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
