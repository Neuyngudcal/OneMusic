package com.example.onemusic.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.search.LibraryGrouping
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import dev.chrisbanes.haze.HazeState


/** HomeSubView.ALBUMS: danh sách album dạng danh sách / lưới 2 cột / lưới 3 cột. Chế độ xem và trạng thái menu do màn cha giữ. */
@Composable
internal fun HomeAlbumsContent(
    tracks: List<Track>,
    albumViewMode: AlbumViewMode,
    onAlbumViewModeChange: (AlbumViewMode) -> Unit,
    isViewMenuExpanded: Boolean,
    onViewMenuExpandedChange: (Boolean) -> Unit,
    hazeState: HazeState,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayTracks: (List<Track>) -> Unit
) {
    // Gộp album bằng hàm dùng chung: theo (tên album, nghệ sĩ chính), sắp xếp A→Z
    val rawAlbums = remember(tracks) {
        LibraryGrouping.groupAlbums(tracks).map { album ->
            AlbumItemData(
                key = album.key,
                name = album.name,
                artist = album.artist,
                trackCount = album.tracks.size,
                artworkUrl = album.tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
                tracks = album.tracks
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
        ) {
            // 1. HEADER (Back, Title, Count, Layout Mode Switcher Button)
            item(key = "albums_header") {
                HomeAlbumsHeader(
                    albumCount = rawAlbums.size,
                    trackCount = tracks.size,
                    albumViewMode = albumViewMode,
                    onAlbumViewModeChange = onAlbumViewModeChange,
                    isViewMenuExpanded = isViewMenuExpanded,
                    onViewMenuExpandedChange = onViewMenuExpandedChange,
                    hazeState = hazeState,
                    onBack = onBack
                )
            }


            // 2. ALBUMS CONTENT BY VIEW MODE
            if (rawAlbums.isEmpty()) {
                item(key = "albums_empty") {
                    HomeAlbumsEmptyState()
                }
            } else {
                when (albumViewMode) {
                    AlbumViewMode.LIST -> {
                        val total = rawAlbums.size
                        itemsIndexed(rawAlbums, key = { index, it -> "list_${it.name}_$index" }) { index, album ->
                            HomeAlbumListRow(
                                album = album,
                                index = index,
                                total = total,
                                onOpenAlbum = onOpenAlbum,
                                onPlayTracks = onPlayTracks
                            )
                        }
                    }

                    AlbumViewMode.GRID_2 -> {
                        val pairs = rawAlbums.chunked(2)
                        itemsIndexed(pairs, key = { idx, pair -> "g2_${idx}_" + pair.joinToString("_") { it.name } }) { _, pair ->
                            HomeAlbumGrid2Row(pair = pair, onOpenAlbum = onOpenAlbum)
                        }
                    }

                    AlbumViewMode.GRID_3 -> {
                        val triplets = rawAlbums.chunked(3)
                        itemsIndexed(triplets, key = { idx, triplet -> "g3_${idx}_" + triplet.joinToString("_") { it.name } }) { _, triplet ->
                            HomeAlbumGrid3Row(triplet = triplet, onOpenAlbum = onOpenAlbum)
                        }
                    }
                }
            }
        }
    }
}
