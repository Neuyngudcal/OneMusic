package com.example.onemusic.ui.screens.library.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.search.AlbumGroup
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.ui.screens.library.LibraryViewMode
import com.example.onemusic.ui.screens.library.items.AlbumGridCard
import com.example.onemusic.ui.screens.library.items.AlbumListItem

/**
 * Extension for rendering Albums tab content inside LibraryScreen's LazyList.
 */
fun LazyListScope.albumsTabContent(
    albums: List<AlbumGroup>,
    libraryViewMode: LibraryViewMode,
    onAlbumClick: (String) -> Unit,
    onPlayAlbum: (List<Track>) -> Unit
) {
    // 1. Section Label
    item(key = "albums_section_label") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DANH SÁCH ALBUM (${albums.size})",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                )
            )
        }
    }

    // 2. Empty State or Albums List / Grid
    if (albums.isEmpty()) {
        item(key = "empty_albums") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .apexGlassCard(shape = RoundedCornerShape(26.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có album nào",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        }
    } else {
        when (libraryViewMode) {
            LibraryViewMode.GRID_2 -> {
                val pairs = albums.chunked(2)
                itemsIndexed(
                    items = pairs,
                    key = { idx, pair -> "lib_album_g2_${idx}_" + pair.joinToString("_") { it.key } }
                ) { _, pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (album in pair) {
                            val artworkUrl = album.tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl
                            AlbumGridCard(
                                albumName = album.name,
                                artistName = album.artist,
                                artworkUrl = artworkUrl,
                                trackCount = album.tracks.size,
                                onClick = { onAlbumClick(album.key) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            LibraryViewMode.GRID_3 -> {
                val triplets = albums.chunked(3)
                itemsIndexed(
                    items = triplets,
                    key = { idx, triplet -> "lib_album_g3_${idx}_" + triplet.joinToString("_") { it.key } }
                ) { _, triplet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (album in triplet) {
                            val artworkUrl = album.tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl
                            AlbumGridCard(
                                albumName = album.name,
                                artistName = album.artist,
                                artworkUrl = artworkUrl,
                                trackCount = album.tracks.size,
                                onClick = { onAlbumClick(album.key) },
                                isCompact = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - triplet.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            LibraryViewMode.LIST -> {
                itemsIndexed(
                    items = albums,
                    key = { index, album -> "album_${album.key}_$index" }
                ) { index, album ->
                    val artworkUrl = album.tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl
                    AlbumListItem(
                        albumName = album.name,
                        artistName = album.artist,
                        artworkUrl = artworkUrl,
                        trackCount = album.tracks.size,
                        index = index,
                        total = albums.size,
                        onClick = { onAlbumClick(album.key) },
                        onPlayClick = {
                            if (album.tracks.isNotEmpty()) {
                                onPlayAlbum(album.tracks)
                            }
                        }
                    )
                }
            }
        }
    }
}
