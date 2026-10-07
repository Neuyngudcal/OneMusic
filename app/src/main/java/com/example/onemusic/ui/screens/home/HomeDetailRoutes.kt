package com.example.onemusic.ui.screens.home

import com.example.onemusic.data.search.LibraryGrouping
import com.example.onemusic.data.repository.ArtistImageRepository
import androidx.compose.material.icons.rounded.Album
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.screens.detail.DetailScreen

// Ba nhánh *_DETAIL của Trang chủ: lọc bài theo nghệ sĩ / album / playlist rồi mở DetailScreen.

@Composable
internal fun HomeArtistDetailRoute(
    selectedArtist: String?,
    tracks: List<Track>,
    artistGroupMode: ArtistGroupMode,
    artistImages: Map<String, String>,
    artistImageRepository: ArtistImageRepository,
    currentTrackId: String?,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onPlayTracks: (List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAddToPlaylist: ((Track) -> Unit)?,
    onPlayNext: ((Track) -> Unit)?,
    onAddToQueue: ((Track) -> Unit)?,
    onBack: () -> Unit
) {
    val artistTracks = remember(selectedArtist, tracks, artistGroupMode) {
        val target = selectedArtist.orEmpty()
        if (artistGroupMode == ArtistGroupMode.MERGED) {
            tracks.filter { track ->
                extractArtistNames(track.artist).any {
                    it.equals(target, ignoreCase = true) ||
                    target.startsWith("$it ", ignoreCase = true) ||
                    target.startsWith("${it}x", ignoreCase = true)
                }
            }
        } else {
            tracks.filter { it.artist.equals(target, ignoreCase = true) }
        }
    }
    val artistImageUrl = selectedArtist?.let {
        artistImages[it.trim().lowercase()] ?: artistImageRepository.getCachedImageUrl(it)
    }
    DetailScreen(
        title = selectedArtist.orEmpty(),
        subtitle = "Nghệ sĩ",
        artworkUrl = artistImageUrl,
        tracks = artistTracks,
        currentTrackId = currentTrackId,
        isArtist = true,
        onTrackSelect = onTrackSelect,
        onPlayAll = onPlayTracks,
        onShuffleAll = { onPlayTracks(it.shuffled()) },
        onToggleFavorite = onToggleFavorite,
        onOpenAddToPlaylist = onOpenAddToPlaylist,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onBack = onBack
    )
}

@Composable
internal fun HomeAlbumDetailRoute(
    selectedAlbum: String?,
    tracks: List<Track>,
    currentTrackId: String?,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onPlayTracks: (List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAddToPlaylist: ((Track) -> Unit)?,
    onPlayNext: ((Track) -> Unit)?,
    onAddToQueue: ((Track) -> Unit)?,
    onBack: () -> Unit
) {
    val albumTracks = remember(selectedAlbum, tracks) {
        LibraryGrouping.tracksOfAlbum(tracks, selectedAlbum.orEmpty())
    }
    DetailScreen(
        title = albumTracks.firstOrNull()?.let { LibraryGrouping.albumName(it) } ?: "Album",
        subtitle = albumTracks.firstOrNull()?.artist ?: "Album",
        artworkUrl = albumTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
        tracks = albumTracks,
        currentTrackId = currentTrackId,
        onTrackSelect = onTrackSelect,
        onPlayAll = onPlayTracks,
        onShuffleAll = { onPlayTracks(it.shuffled()) },
        onToggleFavorite = onToggleFavorite,
        onOpenAddToPlaylist = onOpenAddToPlaylist,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onBack = onBack
    )
}

@Composable
internal fun HomePlaylistDetailRoute(
    isSelectedFavorites: Boolean,
    selectedPlaylistId: String?,
    customPlaylists: List<CustomPlaylist>,
    tracks: List<Track>,
    currentTrackId: String?,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onPlayTracks: (List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAddToPlaylist: ((Track) -> Unit)?,
    onPlayNext: ((Track) -> Unit)?,
    onAddToQueue: ((Track) -> Unit)?,
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onAddTrackToPlaylist: ((String, String) -> Unit)?,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onBack: () -> Unit
) {
    if (isSelectedFavorites) {
        val favTracks = remember(tracks) { tracks.filter { it.isFavorite } }
        DetailScreen(
            title = "Bài hát yêu thích",
            subtitle = "Yêu thích",
            isFavorites = true,
            tracks = favTracks,
            currentTrackId = currentTrackId,
            onTrackSelect = onTrackSelect,
            onPlayAll = onPlayTracks,
            onShuffleAll = { onPlayTracks(it.shuffled()) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onBack = onBack
        )
    } else {
        val activePl = customPlaylists.find { it.id == selectedPlaylistId }
        val plTracks = remember(activePl, tracks) {
            val map = tracks.associateBy { it.id }
            activePl?.trackIds?.mapNotNull { map[it] } ?: emptyList()
        }
        DetailScreen(
            title = activePl?.name ?: "Danh sách phát",
            subtitle = "Danh sách phát",
            customPlaylist = activePl,
            tracks = plTracks,
            currentTrackId = currentTrackId,
            onTrackSelect = onTrackSelect,
            onPlayAll = onPlayTracks,
            onShuffleAll = { onPlayTracks(it.shuffled()) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
            onAddTrackToPlaylist = onAddTrackToPlaylist,
            onRenamePlaylist = onRenamePlaylist,
            onDeletePlaylist = { id ->
                onDeletePlaylist(id)
                onBack()
            },
            onBack = onBack
        )
    }
}
