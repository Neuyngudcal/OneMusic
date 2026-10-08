package com.example.onemusic.ui.screens.search


import com.example.onemusic.data.search.LibraryGrouping

import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.onemusic.data.local.RecentSearchEntry
import com.example.onemusic.data.local.SearchPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.ArtistImageRepository
import com.example.onemusic.data.search.ArtistExtractor
import com.example.onemusic.data.search.MatchedAlbum
import com.example.onemusic.data.search.MatchedArtist
import com.example.onemusic.data.search.MatchedTrack
import com.example.onemusic.data.search.MusicSearchEngine
import com.example.onemusic.data.search.MusicSearchIndex
import com.example.onemusic.data.search.SearchResults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.onemusic.ui.screens.detail.DetailScreen
import dev.chrisbanes.haze.HazeState
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack

enum class SearchFilterTab(val title: String) {
    ALL("Tất cả"),
    SONGS("Bài hát"),
    ARTISTS("Nghệ sĩ"),
    ALBUMS("Album")
}

/**
 * Enhanced Samsung One UI 8.5 Search Screen matching media_1787292102345.jpg:
 * - Category Tab Header ("Tất cả", "Bài hát", "Nghệ sĩ", "Album") with solid pill active indicator and borderless inactive tabs
 * - Powered by Vietnamese-aware, diacritics-stripping, tokenized, fuzzy MusicSearchEngine
 * - Authentic Artist Circles with Monogram/Avatar & Track Count
 * - Grouped Card Song List with Lyrics snippet indicator
 * - Persistent Search History with SharedPreferences
 */
@Composable
fun SearchScreen(
    tracks: List<Track>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    // track = bài được bấm, context = danh sách đang hiển thị (dùng làm hàng đợi)
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onPlayTracks: (List<Track>) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    // Menu thao tác từng bài (nhấn giữ một bài)
    onOpenAddToPlaylist: ((Track) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val searchPrefs = remember { SearchPreferences(context) }
    val artistImageRepository = remember { ArtistImageRepository.getInstance(context) }
    val artistImages by artistImageRepository.artistImagesFlow.collectAsState()

    var selectedFilter by rememberSaveable { mutableStateOf(SearchFilterTab.ALL) }
    var recentSearches by remember { mutableStateOf(emptyList<RecentSearchEntry>()) }
    var selectedArtist by rememberSaveable { mutableStateOf<String?>(null) }
    // Khóa album (LibraryGrouping.albumKey), không phải tên – 2 album khác nhau có thể trùng tên
    var selectedAlbum by rememberSaveable { mutableStateOf<String?>(null) }
    // Bài đang mở menu thao tác (nhấn giữ) – null = đóng
    var trackForActions by remember { mutableStateOf<Track?>(null) }

    // Intercept back gesture if inside artist/album detail
    BackHandler(enabled = selectedArtist != null || selectedAlbum != null) {
        selectedArtist = null
        selectedAlbum = null
    }

    if (selectedArtist != null) {
        val artistTracks = remember(selectedArtist, tracks) {
            val target = selectedArtist.orEmpty()
            tracks.filter { track ->
                ArtistExtractor.extractIndividualArtists(track.artist).any {
                    it.equals(target, ignoreCase = true) ||
                    target.startsWith("$it ", ignoreCase = true) ||
                    target.startsWith("${it}x", ignoreCase = true)
                } || track.artist.contains(target, ignoreCase = true)
            }
        }
        val artistPhotoUrl = artistImages[selectedArtist.orEmpty().trim().lowercase()]
            ?: artistImageRepository.getCachedImageUrl(selectedArtist.orEmpty())

        DetailScreen(
            title = selectedArtist.orEmpty(),
            subtitle = "Nghệ sĩ",
            artworkUrl = artistPhotoUrl,
            tracks = artistTracks,
            currentTrackId = null,
            isArtist = true,
            onTrackSelect = onTrackSelect,
            onPlayAll = onPlayTracks,
            onShuffleAll = { onPlayTracks(it.shuffled()) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onBack = { selectedArtist = null }
        )
        return
    }

    if (selectedAlbum != null) {
        val albumTracks = remember(selectedAlbum, tracks) {
            LibraryGrouping.tracksOfAlbum(tracks, selectedAlbum.orEmpty())
        }
        DetailScreen(
            title = albumTracks.firstOrNull()?.let { LibraryGrouping.albumName(it) } ?: "Album",
            subtitle = albumTracks.firstOrNull()?.artist ?: "Album",
            artworkUrl = albumTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
            tracks = albumTracks,
            currentTrackId = null,
            onTrackSelect = onTrackSelect,
            onPlayAll = onPlayTracks,
            onShuffleAll = { onPlayTracks(it.shuffled()) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onBack = { selectedAlbum = null }
        )
        return
    }

    // Load recent searches on composition
    LaunchedEffect(Unit) {
        recentSearches = searchPrefs.getRecentSearches()
    }

    // Build pre-computed search index in background when tracks change
    var searchIndex by remember { mutableStateOf<MusicSearchIndex?>(null) }
    LaunchedEffect(tracks) {
        withContext(Dispatchers.Default) {
            searchIndex = MusicSearchIndex.build(tracks)
        }
    }

    val allArtists = remember(searchIndex) {
        searchIndex?.allArtists ?: emptyList()
    }

    val allAlbums = remember(searchIndex) {
        searchIndex?.allAlbums ?: emptyList()
    }

    // High performance asynchronous reactive search execution
    var searchResults by remember { mutableStateOf<SearchResults?>(null) }
    // true trong lúc chờ gõ xong / đang tìm → không hiện nhầm "Không tìm thấy kết quả"
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, searchIndex, tracks) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            searchResults = null
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(150L) // debounce: gõ tiếp trong 150ms thì khối này tự hủy và chạy lại
        val idx = searchIndex ?: withContext(Dispatchers.Default) { MusicSearchIndex.build(tracks) }
        searchResults = withContext(Dispatchers.Default) { MusicSearchEngine.search(idx, q) }
        isSearching = false
    }

    val displayTracks: List<MatchedTrack> = remember(searchResults, tracks, searchQuery) {
        if (searchQuery.isNotBlank()) {
            searchResults?.tracks ?: emptyList()
        } else {
            tracks.map { MatchedTrack(it, 0) }
        }
    }
    // Danh sách bài đang hiển thị → dùng làm hàng đợi khi bấm một kết quả
    val displayTrackList: List<Track> = remember(displayTracks) { displayTracks.map { it.track } }

    val displayArtists: List<MatchedArtist> = remember(searchResults, allArtists, searchQuery) {
        if (searchQuery.isNotBlank()) {
            searchResults?.artists ?: emptyList()
        } else {
            allArtists
        }
    }

    val displayAlbums: List<MatchedAlbum> = remember(searchResults, allAlbums, searchQuery) {
        if (searchQuery.isNotBlank()) {
            searchResults?.albums ?: emptyList()
        } else {
            allAlbums
        }
    }

    val hasAnySearchResults = remember(searchResults) {
        searchResults?.let { it.tracks.isNotEmpty() || it.artists.isNotEmpty() || it.albums.isNotEmpty() } ?: false
    }
    // Xét kết quả theo TAB đang chọn (vd tab Nghệ sĩ mà chỉ có kết quả bài hát → vẫn là "không tìm thấy")
    val currentTabHasResults = when (selectedFilter) {
        SearchFilterTab.ALL -> hasAnySearchResults
        SearchFilterTab.SONGS -> displayTracks.isNotEmpty()
        SearchFilterTab.ARTISTS -> displayArtists.isNotEmpty()
        SearchFilterTab.ALBUMS -> displayAlbums.isNotEmpty()
    }

    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }

    // Mở tab Tìm kiếm (ô đang trống) → tự focus ô nhập để bàn phím bật lên ngay
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (searchQuery.isEmpty()) runCatching { focusRequester.requestFocus() }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val stableStatusBarTop = remember(view, density) {
        val rootInsets = androidx.core.view.ViewCompat.getRootWindowInsets(view)
        val topPx = rootInsets?.getInsetsIgnoringVisibility(androidx.core.view.WindowInsetsCompat.Type.statusBars())?.top
            ?: rootInsets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())?.top
            ?: 0
        if (topPx > 0) with(density) { topPx.toDp() } else 40.dp
    }

    // Lưu từ khóa (kèm nhãn "Album"/"Nghệ sĩ"/"Bài hát") khi mở một kết quả
    val recordRecentSearch: (String, String) -> Unit = { query, label ->
        searchPrefs.addRecentSearch(query, label)
        recentSearches = searchPrefs.getRecentSearches()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = stableStatusBarTop)
        ) {
            // 1. Top Search Bar Header (Pinned rock-solid at the top)
            SearchInputBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                // Nút "Tìm" trên bàn phím: lưu từ khóa vào lịch sử
                onSubmitSearch = {
                    val q = searchQuery.trim()
                    if (q.isNotEmpty()) {
                        searchPrefs.addRecentSearch(q)
                        recentSearches = searchPrefs.getRecentSearches()
                    }
                },
                focusRequester = focusRequester
            )

            // 2. Category Filter Pills Row (Tất cả, Bài hát, Nghệ sĩ, Album)
            SearchFilterTabsRow(selectedFilter = selectedFilter, onSelectFilter = { selectedFilter = it })

            // MAIN CONTENT LIST (Recent Searches OR Categorized Search Results)
            val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
            val bottomContentPadding = if (isKeyboardOpen) 24.dp else LocalBottomOverlayPadding.current

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
                contentPadding = PaddingValues(bottom = bottomContentPadding, top = 4.dp)
            ) {
                if (searchQuery.isBlank()) {
                    searchBlankState(
                        selectedFilter = selectedFilter,
                        recentSearches = recentSearches,
                        allArtists = allArtists,
                        allAlbums = allAlbums,
                        displayTracks = displayTracks,
                        displayTrackList = displayTrackList,
                        tracks = tracks,
                        artistImages = artistImages,
                        artistImageRepository = artistImageRepository,
                        onSearchQueryChange = onSearchQueryChange,
                        onClearAllRecentSearches = {
                            searchPrefs.clearAllRecentSearches()
                            recentSearches = emptyList()
                        },
                        onRemoveRecentSearch = { query ->
                            searchPrefs.removeRecentSearch(query)
                            recentSearches = searchPrefs.getRecentSearches()
                        },
                        onRecordRecentSearch = recordRecentSearch,
                        onOpenArtist = { selectedArtist = it },
                        onOpenAlbum = { selectedAlbum = it },
                        onTrackSelect = onTrackSelect,
                        onToggleFavorite = onToggleFavorite,
                        onTrackLongClick = { trackForActions = it },
                        onPlayTracks = onPlayTracks
                    )
                } else {
                    searchResultsSection(
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        isSearching = isSearching,
                        currentTabHasResults = currentTabHasResults,
                        displayArtists = displayArtists,
                        displayAlbums = displayAlbums,
                        displayTracks = displayTracks,
                        displayTrackList = displayTrackList,
                        tracks = tracks,
                        artistImages = artistImages,
                        artistImageRepository = artistImageRepository,
                        onRecordRecentSearch = recordRecentSearch,
                        onOpenArtist = { selectedArtist = it },
                        onOpenAlbum = { selectedAlbum = it },
                        onTrackSelect = onTrackSelect,
                        onToggleFavorite = onToggleFavorite,
                        onTrackLongClick = { trackForActions = it },
                        onPlayTracks = onPlayTracks
                    )
                }
            }
        }

        // Menu thao tác của bài vừa nhấn giữ (Phát kế tiếp, Thêm vào playlist, Thông tin bài hát...)
        com.example.onemusic.ui.components.TrackActionMenu(
            track = trackForActions,
            onDismiss = { trackForActions = null },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue
        )
    }
}