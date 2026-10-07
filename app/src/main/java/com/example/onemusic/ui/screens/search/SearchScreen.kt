package com.example.onemusic.ui.screens.search

import androidx.compose.material3.minimumInteractiveComponentSize

import com.example.onemusic.data.search.LibraryGrouping

import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import androidx.activity.compose.BackHandler
import dev.chrisbanes.haze.hazeSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.example.onemusic.ui.components.TrackDetailsDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.ui.components.ApexHiResBadge
import coil.compose.AsyncImage
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.screens.detail.DetailScreen
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import com.example.onemusic.theme.avatarColorFor
import com.example.onemusic.theme.ApexPillBorderBrush
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ShadowColor
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.TextTertiary
import com.example.onemusic.theme.apexFrostedGlass
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.theme.apexGroupedCardItem

enum class SearchFilterTab(val title: String) {
    ALL("Tất cả"),
    SONGS("Bài hát"),
    ARTISTS("Nghệ sĩ"),
    ALBUMS("Album")
}

private fun getAvatarColorForArtist(name: String): Color {
    return avatarColorFor(name)
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

    @OptIn(ExperimentalFoundationApi::class)
    val noOpBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = localRect
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                // Prevent Compose from nudging/scrolling the search bar downwards on focus
            }
        }
    }

    // Mở tab Tìm kiếm (ô đang trống) → tự focus ô nhập để bàn phím bật lên ngay
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp)
                    .height(52.dp)
                    .shadow(elevation = 8.dp, shape = PillShape, ambientColor = ShadowColor)
                    .clip(PillShape)
                    .background(SurfaceElevated.copy(alpha = 0.88f))
                    .border(0.85.dp, ApexPillBorderBrush, PillShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Tìm kiếm",
                        tint = IvoryHigh,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Nghệ sĩ, bài hát, album...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary, // TextTertiary chỉ ~3:1, không đạt WCAG AA
                                    fontSize = 15.sp
                                )
                            )
                        }

                        @OptIn(ExperimentalFoundationApi::class)
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(TextPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            // Nút "Tìm" trên bàn phím: ẩn bàn phím và lưu từ khóa vào lịch sử
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                val q = searchQuery.trim()
                                if (q.isNotEmpty()) {
                                    searchPrefs.addRecentSearch(q)
                                    recentSearches = searchPrefs.getRecentSearches()
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .bringIntoViewResponder(noOpBringIntoViewResponder)
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        ApexCircularGlassButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Xóa tìm kiếm",
                            onClick = { onSearchQueryChange("") },
                            size = 32.dp,
                            iconSize = 16.dp,
                            modifier = Modifier.minimumInteractiveComponentSize(), // vùng chạm ≥ 48dp
                            iconTint = PrimaryIvory.copy(alpha = 0.85f),
                            backgroundColor = SurfaceActiveIndicator.copy(alpha = 0.60f)
                        )
                    }
                }
            }

            // 2. Category Filter Pills Row (Tất cả, Bài hát, Nghệ sĩ, Album)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(SearchFilterTab.entries.toTypedArray(), key = { it.name }) { tab ->
                    val isSelected = selectedFilter == tab
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryIvory else SurfaceCard,
                        animationSpec = spring(stiffness = 500f),
                        label = "tab_pill_bg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) Color.Transparent else SurfaceBorderStrong,
                        animationSpec = spring(stiffness = 500f),
                        label = "tab_pill_border"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) CharcoalBlack else TextSecondary,
                        animationSpec = spring(stiffness = 500f),
                        label = "tab_pill_text"
                    )
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(PillShape)
                            .background(bgColor)
                            .border(0.8.dp, borderColor, PillShape)
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                selectedFilter = tab
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = textColor,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

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
                    // ==========================================
                    // --- BLANK QUERY STATE (BY SELECTED TAB) ---
                    // ==========================================
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
                                                color = TextPrimary,
                                                fontSize = 20.sp
                                            )
                                        )
                                        Text(
                                            text = "Xóa tất cả",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = ApexRose,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            modifier = Modifier
                                                .apexBounceClick(scaleDown = 0.90f) {
                                                    searchPrefs.clearAllRecentSearches()
                                                    recentSearches = emptyList()
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
                                                        .background(SurfaceActiveIndicator),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.History,
                                                        contentDescription = null,
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.query,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextPrimary,
                                                            fontSize = 16.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = item.subtitle,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = TextSecondary,
                                                            fontSize = 13.sp
                                                        ),
                                                        maxLines = 1
                                                    )
                                                }

                                                ApexCircularGlassButton(
                                                    icon = Icons.Rounded.Close,
                                                    contentDescription = "Xóa khỏi lịch sử",
                                                    onClick = {
                                                        searchPrefs.removeRecentSearch(item.query)
                                                        recentSearches = searchPrefs.getRecentSearches()
                                                    },
                                                    size = 34.dp,
                                                    iconSize = 16.dp,
                                                    modifier = Modifier.minimumInteractiveComponentSize(), // vùng chạm ≥ 48dp
                                                    iconTint = TextSecondary.copy(alpha = 0.85f),
                                                    backgroundColor = SurfaceActiveIndicator.copy(alpha = 0.60f)
                                                )
                                            }

                                            if (index < totalRecents - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                                    thickness = 0.6.dp,
                                                    color = SurfaceDivider
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
                                                tint = com.example.onemusic.theme.SurfaceActiveIndicator,
                                                modifier = Modifier.size(54.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Tìm kiếm bài hát, nghệ sĩ hoặc lời bài hát",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextSecondary,
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
                                                selectedArtist = artist.artistName
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
                                            onLongClick = { trackForActions = matchedItem.track }
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
                                                searchPrefs.addRecentSearch(album.albumName, "Album")
                                                recentSearches = searchPrefs.getRecentSearches()
                                                selectedAlbum = album.albumKey
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
                } else {
                    // ==========================================
                    // --- LIVE SEARCH RESULTS STATE ---
                    // ==========================================
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
                                                searchPrefs.addRecentSearch(artist.artistName, "Nghệ sĩ")
                                                recentSearches = searchPrefs.getRecentSearches()
                                                selectedArtist = artist.artistName
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
                                                searchPrefs.addRecentSearch(artist.artistName, "Nghệ sĩ")
                                                recentSearches = searchPrefs.getRecentSearches()
                                                selectedArtist = artist.artistName
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
                                                searchPrefs.addRecentSearch(album.albumName, "Album")
                                                recentSearches = searchPrefs.getRecentSearches()
                                                selectedAlbum = album.albumKey
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
                                                searchPrefs.addRecentSearch(album.albumName, "Album")
                                                recentSearches = searchPrefs.getRecentSearches()
                                                selectedAlbum = album.albumKey
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
                                                searchPrefs.addRecentSearch(track.title, "Bài hát")
                                                recentSearches = searchPrefs.getRecentSearches()
                                                onTrackSelect(track, displayTrackList)
                                            },
                                            onToggleFavorite = { onToggleFavorite(track.id) },
                                            onLongClick = { trackForActions = track }
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

@Composable
private fun SearchAlbumCard(
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
                .background(SurfaceActiveIndicator)
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
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = IvoryDisabled,
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
                color = TextPrimary,
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
                color = TextSecondary,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistRow(
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
                    .border(0.7.dp, IvoryStroke, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(com.example.onemusic.theme.SurfaceActiveIndicator),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = PrimaryIvory,
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
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$trackCount bài hát",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ArtistCard(
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
                        .border(0.7.dp, IvoryStroke, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(com.example.onemusic.theme.SurfaceActiveIndicator),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = PrimaryIvory,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = artist.artistName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
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
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrackResultRow(
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
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
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
                        tint = Brand,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "\"${matchedTrack.matchedLyricSnippet}\"",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = IvoryHigh,
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
                    tint = if (track.isFavorite) ApexRose else IvoryFaint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

