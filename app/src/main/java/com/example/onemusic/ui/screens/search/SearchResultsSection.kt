package com.example.onemusic.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.ArtistImageRepository
import com.example.onemusic.data.search.MatchedAlbum
import com.example.onemusic.data.search.MatchedArtist
import com.example.onemusic.data.search.MatchedTrack
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGroupedCardItem
import androidx.compose.foundation.lazy.LazyListScope

/** Kết quả tìm kiếm trực tiếp: spinner, "không tìm thấy", rồi các nhóm Nghệ sĩ / Album / Bài hát theo tab. */
internal fun LazyListScope.searchResultsSection(
    searchQuery: String,
    selectedFilter: SearchFilterTab,
    isSearching: Boolean,
    currentTabHasResults: Boolean,
    displayArtists: List<MatchedArtist>,
    displayAlbums: List<MatchedAlbum>,
    displayTracks: List<MatchedTrack>,
    displayTrackList: List<Track>,
    tracks: List<Track>,
    artistImages: Map<String, String>,
    artistImageRepository: ArtistImageRepository,
    onRecordRecentSearch: (query: String, label: String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onPlayTracks: (List<Track>) -> Unit
) {
    if (isSearching && !currentTabHasResults) {
        // Đang chờ gõ xong / đang tìm và chưa có gì để hiện → spinner thay vì nháy "Không tìm thấy"
        item(key = "searching") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Brand,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    } else if (!currentTabHasResults) {
        item(key = "empty_search_result") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 60.dp, horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Không tìm thấy kết quả nào cho \"$searchQuery\"",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kiểm tra lại chính tả hoặc thử tìm kiếm bằng tên nghệ sĩ, bài hát hoặc từ khóa khác.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        // 1. SECTION: MATCHED ARTISTS (Nghệ sĩ)
        if (selectedFilter == SearchFilterTab.ARTISTS && displayArtists.isNotEmpty()) {
            item(key = "artists_section_header_tab") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nghệ sĩ (${displayArtists.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                }
            }
            val totalArtists = displayArtists.size
            itemsIndexed(displayArtists, key = { index, it -> "${it.artistName}_$index" }) { index, artist ->
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
                                onRecordRecentSearch(artist.artistName, "Nghệ sĩ")
                                onOpenArtist(artist.artistName)
                            }
                        )

                        if (index < totalArtists - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 98.dp, end = 16.dp),
                                thickness = 0.6.dp,
                                color = SurfaceDivider
                            )
                        }
                    }
                }
            }
        } else if (selectedFilter == SearchFilterTab.ALL && displayArtists.isNotEmpty()) {
            item(key = "artists_section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nghệ sĩ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            // Carousel of Artist Cards
            item(key = "artists_carousel") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayArtists, key = { it.artistName }) { artist ->
                        val artistImageUrl = artistImages[artist.artistName.trim().lowercase()]
                            ?: artistImageRepository.getCachedImageUrl(artist.artistName)

                        ArtistCard(
                            artist = artist,
                            artistImageUrl = artistImageUrl,
                            onClick = {
                                onRecordRecentSearch(artist.artistName, "Nghệ sĩ")
                                onOpenArtist(artist.artistName)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 2. SECTION: MATCHED ALBUMS (Album)
        if (selectedFilter == SearchFilterTab.ALBUMS && displayAlbums.isNotEmpty()) {
            item(key = "albums_section_header_tab") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Album (${displayAlbums.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            val pairs = displayAlbums.chunked(2)
            items(pairs, key = { pair -> "search_album_pair_" + pair.joinToString("_") { it.albumKey } }) { pair ->
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
        } else if (selectedFilter == SearchFilterTab.ALL && displayAlbums.isNotEmpty()) {
            item(key = "albums_section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Album",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            item(key = "albums_carousel") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayAlbums, key = { it.albumKey }) { album ->
                        SearchAlbumCard(
                            album = album,
                            allTracks = tracks,
                            onClick = {
                                onRecordRecentSearch(album.albumName, "Album")
                                onOpenAlbum(album.albumKey)
                            },
                            onPlayTracks = onPlayTracks,
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 3. SECTION: MATCHED SONGS (Grouped Card Container)
        if ((selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.SONGS) && displayTracks.isNotEmpty()) {
            item(key = "songs_section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bài hát",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${displayTracks.size})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            itemsIndexed(
                items = displayTracks,
                key = { index, item -> "search_track_${item.track.id}_$index" }
            ) { index, matchedItem ->
                val track = matchedItem.track
                val total = displayTracks.size
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .apexGroupedCardItem(index = index, total = total, cornerRadius = 26.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TrackResultRow(
                            matchedTrack = matchedItem,
                            onClick = {
                                onRecordRecentSearch(track.title, "Bài hát")
                                onTrackSelect(track, displayTrackList)
                            },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onLongClick = { onTrackLongClick(track) }
                        )
                        if (index < total - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 102.dp, end = 16.dp),
                                thickness = 0.6.dp,
                                color = SurfaceDivider
                            )
                        }
                    }
                }
            }
        }
    }
}
