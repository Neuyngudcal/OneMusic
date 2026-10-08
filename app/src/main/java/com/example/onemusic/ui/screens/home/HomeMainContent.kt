package com.example.onemusic.ui.screens.home

import com.example.onemusic.theme.AppTheme
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import com.example.onemusic.data.repository.ArtistImageRepository
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.apexGlassCard
import androidx.compose.foundation.lazy.LazyListState

/** Màn chính của Trang chủ (HomeSubView.MAIN): lời chào, banner nổi bật, các hàng ngang và lối tắt. */
@Composable
internal fun HomeMainContent(
    listState: LazyListState,
    tracks: List<Track>,
    currentTrackId: String?,
    greetingTitle: String,
    greetingSubtitle: String,
    featuredTrack: Track?,
    recentlyPlayedTracks: List<Track>,
    suggestedTracks: List<Track>,
    customPlaylists: List<CustomPlaylist>,
    favoriteTrackCount: Int,
    topArtists: List<Pair<String, Int>>,
    artistImages: Map<String, String>,
    artistImageRepository: ArtistImageRepository,
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onScanLibrary: () -> Unit,
    onOpenFolders: () -> Unit,
    onNavigate: (HomeSubView) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylist: (CustomPlaylist) -> Unit,
    onOpenArtist: (String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
    ) {
        // 1. Top Header: Minimalist Clean Header with Compact Status Bar Padding
        item(key = "home_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
            ) {
                Text(
                    text = greetingTitle,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 32.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = greetingSubtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp
                    )
                )
            }
        }

        // 1.2 Thư viện trống (lần mở app đầu tiên): hướng dẫn quét nhạc thay vì màn trống trơn
        if (tracks.isEmpty()) {
            item(key = "home_empty_library") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .apexGlassCard(shape = RoundedCornerShape(26.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Thư viện đang trống",
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Quét bộ nhớ máy hoặc chọn thư mục chứa nhạc để bắt đầu.",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(AppTheme.colors.textPrimary)
                            .apexBounceClick { onScanLibrary() }
                            .padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Text("Quét nhạc trên máy", color = AppTheme.colors.onInverse, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Chọn thư mục",
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .apexBounceClick { onOpenFolders() }
                            .padding(8.dp)
                    )
                }
            }
            }

        homeHeroBannerSection(featuredTrack, tracks, onTrackSelect)

        // Thư viện trống → ẩn "Nghe gần đây" và "Có thể bạn sẽ thích" cho màn trống gọn gàng
        if (tracks.isNotEmpty()) {
            homeRecentSection(recentlyPlayedTracks, suggestedTracks, currentTrackId, onTrackSelect, onTrackLongClick)
        }

        homePlaylistsCarouselSection(
            customPlaylists = customPlaylists,
            favoriteTrackCount = favoriteTrackCount,
            onSeeAll = { onNavigate(HomeSubView.PLAYLISTS) },
            onOpenFavorites = onOpenFavorites,
            onOpenPlaylist = onOpenPlaylist
        )

        homeTopArtistsSection(
            topArtists = topArtists,
            artistImages = artistImages,
            artistImageRepository = artistImageRepository,
            onSeeAll = { onNavigate(HomeSubView.ARTISTS) },
            onOpenArtist = onOpenArtist
        )

        homeExploreSection(onNavigate)
    }
}
