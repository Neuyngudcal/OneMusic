package com.example.onemusic.ui.screens.library

import com.example.onemusic.theme.AppTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.ArtistImageRepository
import com.example.onemusic.data.search.LibraryGrouping
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.ui.components.AddToPlaylistMultipleDialog
import com.example.onemusic.ui.components.TrackActionMenu
import com.example.onemusic.ui.screens.detail.DetailScreen
import com.example.onemusic.ui.screens.library.components.FastAlphabetScroller
import com.example.onemusic.ui.screens.library.components.LibraryBatchActionBar
import com.example.onemusic.ui.screens.library.components.LibraryCollapsibleHeader
import com.example.onemusic.ui.screens.library.components.LibraryFilterBar
import com.example.onemusic.ui.screens.library.components.LibraryTabRow
import com.example.onemusic.ui.screens.library.tabs.LibraryNewPlaylistDialog
import com.example.onemusic.ui.screens.library.tabs.SongsDragSelectRail
import com.example.onemusic.ui.screens.library.tabs.albumsTabContent
import com.example.onemusic.ui.screens.library.tabs.artistsTabContent
import com.example.onemusic.ui.screens.library.tabs.playlistsTabContent
import com.example.onemusic.ui.screens.library.tabs.songsTabContent
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import com.example.onemusic.ui.utils.rememberScanWithPermission
import dev.chrisbanes.haze.HazeState

/**
 * Number of LazyColumn items above the first song on the Songs tab:
 * "library_header", "library_main_filter_tabs", "filter_tabs_row".
 * Update when adding/removing items there, or the alphabet scroller jumps to the wrong song.
 */
private const val SONGS_HEADER_ITEM_COUNT = 3

/**
 * Main Library Screen: Clean, modular architecture conforming to Samsung One UI 8.5 aesthetics.
 * - Modular components: Collapsible Header, Pill Tab Row, Filter Bar, Batch Dock, Fast Scroller.
 * - Modular tabs: Songs, Albums (Squircle Vinyl), Artists (Soft Avatar), Playlists (Custom & M3U).
 * - State management delegated to [LibraryViewModel].
 */
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    currentTrackId: String?,
    customPlaylists: List<CustomPlaylist> = emptyList(),
    onCreatePlaylist: (String) -> CustomPlaylist? = { null },
    onAddToPlaylist: (playlistId: String, trackId: String) -> Boolean = { _, _ -> false },
    onPlayTracks: ((List<Track>) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onPlayNextTracks: ((List<Track>) -> Unit)? = null,
    onOpenAddToPlaylist: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    onImportPlaylistM3u: (() -> Unit)? = null,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRescan: () -> Unit = {},
    onOpenFolders: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onDeletePlaylist: ((CustomPlaylist) -> Unit)? = null,
    onExportPlaylistM3u: ((CustomPlaylist) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val settingsPreferences = remember { SettingsPreferences(context) }
    val appSettings by settingsPreferences.settingsFlow.collectAsState()

    val artistImageRepository = remember { ArtistImageRepository.getInstance(context) }
    val artistImages by artistImageRepository.artistImagesFlow.collectAsState()

    val uiState by viewModel.uiState.collectAsState()

    // Initialize saved view mode on first load
    LaunchedEffect(appSettings.libraryViewMode) {
        viewModel.initViewMode(appSettings.libraryViewMode)
    }

    // Back handling for navigation and batch selection
    val isAnyDetailOpen = uiState.openedAlbumKey != null || uiState.openedArtistName != null || uiState.openedPlaylistId != null
    BackHandler(enabled = isAnyDetailOpen || uiState.isMultiSelectMode) {
        if (uiState.isMultiSelectMode) {
            viewModel.exitMultiSelect()
        } else {
            viewModel.closeAllDetails()
        }
    }

    // Scan permission handler
    val triggerScanWithPermission = rememberScanWithPermission(onRescan)

    // Data groupings
    val albums = remember(tracks) { LibraryGrouping.groupAlbums(tracks) }
    val artists = remember(tracks) { LibraryGrouping.groupArtists(tracks) }

    // Sorted tracks computation
    val sortedTracks = remember(tracks, uiState.sortOption, uiState.sortAscending) {
        LibraryViewModel.computeSortedTracks(tracks, uiState.sortOption, uiState.sortAscending)
    }

    // Available letters for fast scroller
    val availableLetters = remember(sortedTracks) {
        LibraryViewModel.computeAvailableLetters(sortedTracks)
    }

    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }

    val currentHeaderSubtitle = remember(uiState.currentTab, tracks.size, albums.size, artists.size, customPlaylists.size) {
        when (uiState.currentTab) {
            LibraryTab.SONGS -> "${tracks.size} bài hát"
            LibraryTab.ALBUMS -> "${albums.size} album"
            LibraryTab.ARTISTS -> "${artists.size} nghệ sĩ"
            LibraryTab.PLAYLISTS -> "${1 + customPlaylists.size} playlist"
        }
    }

    val playList: (List<Track>) -> Unit = { list ->
        if (list.isNotEmpty()) onPlayTracks?.invoke(list) ?: onTrackSelect(list.first(), list)
    }

    // Detail screens navigation
    uiState.openedAlbumKey?.let { albumKey ->
        val album = albums.firstOrNull { it.key == albumKey }
        val albumTracks = album?.tracks.orEmpty()
        DetailScreen(
            title = album?.name ?: "Album",
            subtitle = albumTracks.firstOrNull()?.artist ?: "Album",
            artworkUrl = albumTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
            tracks = albumTracks,
            currentTrackId = currentTrackId,
            onTrackSelect = onTrackSelect,
            onPlayAll = playList,
            onShuffleAll = { playList(it.shuffled()) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onBack = { viewModel.closeAlbum() }
        )
        return
    }

    uiState.openedArtistName?.let { artistName ->
        val artistTracks = artists.firstOrNull { it.first == artistName }?.second.orEmpty()
        DetailScreen(
            title = artistName,
            subtitle = "Nghệ sĩ",
            artworkUrl = artistImages[artistName.trim().lowercase()] ?: artistImageRepository.getCachedImageUrl(artistName),
            tracks = artistTracks,
            currentTrackId = currentTrackId,
            isArtist = true,
            onTrackSelect = onTrackSelect,
            onPlayAll = playList,
            onShuffleAll = { playList(it.shuffled()) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onBack = { viewModel.closeArtist() }
        )
        return
    }

    uiState.openedPlaylistId?.let { playlistId ->
        if (playlistId == "FAVORITES") {
            val favoriteTracks = tracks.filter { it.isFavorite }
            DetailScreen(
                title = "Bài hát yêu thích",
                subtitle = "Playlist",
                tracks = favoriteTracks,
                currentTrackId = currentTrackId,
                isFavorites = true,
                onTrackSelect = onTrackSelect,
                onPlayAll = playList,
                onShuffleAll = { playList(it.shuffled()) },
                onToggleFavorite = onToggleFavorite,
                onOpenAddToPlaylist = onOpenAddToPlaylist,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onBack = { viewModel.closePlaylist() }
            )
            return
        } else {
            val activePl = customPlaylists.find { it.id == playlistId }
            val plTracks = tracks.filter { it.id in (activePl?.trackIds ?: emptyList()) }
            DetailScreen(
                title = activePl?.name ?: "Playlist",
                subtitle = "Danh sách phát",
                tracks = plTracks,
                currentTrackId = currentTrackId,
                customPlaylist = activePl,
                onTrackSelect = onTrackSelect,
                onPlayAll = playList,
                onShuffleAll = { playList(it.shuffled()) },
                onToggleFavorite = onToggleFavorite,
                onOpenAddToPlaylist = onOpenAddToPlaylist,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onBack = { viewModel.closePlaylist() }
            )
            return
        }
    }

    // Main Library content
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
        ) {
            // 1. Top Header: Collapsible Large Title
            item(key = "library_header") {
                LibraryCollapsibleHeader(
                    title = "Thư viện",
                    subtitle = currentHeaderSubtitle,
                    isMultiSelectMode = uiState.isMultiSelectMode,
                    hazeState = hazeState,
                    onToggleMultiSelect = { viewModel.toggleMultiSelect() },
                    onRescan = triggerScanWithPermission,
                    onImportPlaylistM3u = onImportPlaylistM3u,
                    onOpenFolders = onOpenFolders
                )
            }

            // 2. Main Filter Tabs: One UI Pill Group
            item(key = "library_main_filter_tabs") {
                LibraryTabRow(
                    currentTab = uiState.currentTab,
                    onTabSelected = { tab -> viewModel.setTab(tab) }
                )
            }

            // 3. Tab Contents
            when (uiState.currentTab) {
                LibraryTab.SONGS -> {
                    // Toolbar: sort, view mode, shuffle, play all
                    item(key = "filter_tabs_row") {
                        LibraryFilterBar(
                            viewMode = uiState.viewMode,
                            onViewModeChange = { mode -> viewModel.setViewMode(mode, settingsPreferences) },
                            hazeState = hazeState,
                            currentSortOption = uiState.sortOption,
                            sortAscending = uiState.sortAscending,
                            onSortOptionChange = { opt -> viewModel.setSortOption(opt) },
                            onPlayAll = {
                                if (sortedTracks.isNotEmpty()) {
                                    onTrackSelect(sortedTracks.first(), sortedTracks)
                                }
                            },
                            onShuffleAll = onShuffleAll
                        )
                    }

                    // Songs list / grid
                    songsTabContent(
                        sortedTracks = sortedTracks,
                        currentTrackId = currentTrackId,
                        libraryViewMode = uiState.viewMode,
                        isMultiSelectMode = uiState.isMultiSelectMode,
                        selectedTrackIds = uiState.selectedTrackIds,
                        isHiResBadgeEnabled = appSettings.isHiResBadgeEnabled,
                        onTrackSelect = { track -> onTrackSelect(track, sortedTracks) },
                        onTrackLongClick = { track ->
                            if (!uiState.isMultiSelectMode) {
                                viewModel.setTrackForActions(track)
                            }
                        },
                        onToggleFavorite = onToggleFavorite,
                        onTrackCheckedChange = { trackId -> viewModel.toggleTrackSelection(trackId) },
                        onRescan = triggerScanWithPermission
                    )
                }

                LibraryTab.ALBUMS -> {
                    albumsTabContent(
                        albums = albums,
                        libraryViewMode = uiState.viewMode,
                        onViewModeChange = { mode -> viewModel.setViewMode(mode, settingsPreferences) },
                        onAlbumClick = { albumKey -> viewModel.openAlbum(albumKey) }
                    )
                }

                LibraryTab.ARTISTS -> {
                    artistsTabContent(
                        artists = artists,
                        artistImages = artistImages,
                        getCachedArtistImageUrl = { name -> artistImageRepository.getCachedImageUrl(name) },
                        onArtistClick = { artistName -> viewModel.openArtist(artistName) }
                    )
                }

                LibraryTab.PLAYLISTS -> {
                    playlistsTabContent(
                        customPlaylists = customPlaylists,
                        tracks = tracks,
                        onOpenFavoritePlaylist = { viewModel.openPlaylist("FAVORITES") },
                        onOpenPlaylistDetail = { pl -> viewModel.openPlaylist(pl.id) },
                        onOpenNewPlaylistDialog = { viewModel.setShowNewPlaylistDialog(true) },
                        onExportPlaylistM3u = onExportPlaylistM3u,
                        onDeletePlaylist = onDeletePlaylist
                    )
                }
            }
        }

        // Drag-to-Select Multi-items Gesture Rail (Left Checkbox Rail)
        if (uiState.isMultiSelectMode && uiState.currentTab == LibraryTab.SONGS && uiState.viewMode == LibraryViewMode.LIST) {
            SongsDragSelectRail(
                isMultiSelectMode = uiState.isMultiSelectMode,
                sortedTracks = sortedTracks,
                selectedTrackIds = uiState.selectedTrackIds,
                listState = listState,
                scope = scope,
                onSelectionChanged = { ids -> viewModel.setSelectedTrackIds(ids) },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        // Alphabet Fast Scroller (Right Edge Rail)
        if (uiState.currentTab == LibraryTab.SONGS &&
            !uiState.isMultiSelectMode &&
            uiState.viewMode == LibraryViewMode.LIST &&
            sortedTracks.size >= 10 &&
            uiState.sortOption == SongSortOption.TITLE_AZ
        ) {
            FastAlphabetScroller(
                availableLetters = availableLetters,
                sortedTracks = sortedTracks,
                listState = listState,
                headerOffsetCount = SONGS_HEADER_ITEM_COUNT,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Floating Batch Actions Pill Dock (Multi-Select Mode)
        LibraryBatchActionBar(
            isVisible = uiState.isMultiSelectMode,
            selectedCount = uiState.selectedTrackIds.size,
            totalCount = sortedTracks.size,
            onClose = { viewModel.exitMultiSelect() },
            onToggleSelectAll = { viewModel.selectAllTracks(sortedTracks.map { it.id }) },
            onPlaySelected = {
                val tracksToPlay = sortedTracks.filter { it.id in uiState.selectedTrackIds }
                if (tracksToPlay.isNotEmpty()) {
                    onPlayTracks?.invoke(tracksToPlay) ?: onTrackSelect(tracksToPlay.first(), tracksToPlay)
                    viewModel.exitMultiSelect()
                }
            },
            onPlayNextSelected = if (onPlayNextTracks != null) {
                {
                    val tracksToPlayNext = sortedTracks.filter { it.id in uiState.selectedTrackIds }
                    if (tracksToPlayNext.isNotEmpty()) {
                        onPlayNextTracks(tracksToPlayNext)
                        viewModel.exitMultiSelect()
                    }
                }
            } else null,
            onAddToPlaylist = { viewModel.setShowBatchAddToPlaylistDialog(true) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Batch Add to Playlist Dialog
        if (uiState.showBatchAddToPlaylistDialog) {
            val selectedTracks = sortedTracks.filter { it.id in uiState.selectedTrackIds }
            AddToPlaylistMultipleDialog(
                tracks = selectedTracks,
                playlists = customPlaylists,
                onCreatePlaylist = onCreatePlaylist,
                onAddToPlaylist = onAddToPlaylist,
                onAdded = { viewModel.exitMultiSelect() },
                onDismiss = { viewModel.setShowBatchAddToPlaylistDialog(false) }
            )
        }

        // Single Track Action Menu
        TrackActionMenu(
            track = uiState.trackForActions,
            onDismiss = { viewModel.setTrackForActions(null) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue
        )

        // New Playlist Dialog
        if (uiState.showNewPlaylistDialog) {
            LibraryNewPlaylistDialog(
                hazeState = hazeState,
                onDismissRequest = { viewModel.setShowNewPlaylistDialog(false) },
                onCreatePlaylist = { name -> onCreatePlaylist(name) }
            )
        }
    }
}
