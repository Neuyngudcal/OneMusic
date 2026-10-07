package com.example.onemusic.ui.screens.home

import com.example.onemusic.data.search.LibraryGrouping
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.onemusic.data.model.Track

data class HomeCategory(
    val title: String,
    val icon: ImageVector
)

data class AlbumItemData(
    /** Khóa album (LibraryGrouping.albumKey) – dùng để mở trang chi tiết */
    val key: String,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val artworkUrl: String?,
    val tracks: List<Track>
)

data class ArtistItemData(
    val name: String,
    val trackCount: Int,
    val tracks: List<Track>
)

enum class ArtistGroupMode(val title: String, val subtitle: String) {
    MERGED("Gộp nghệ sĩ trùng tên", "Tự động gộp bài hát cùng nghệ sĩ & tách bài hợp tác (feat, &)"),
    SEPARATE("Tách riêng theo thẻ gốc", "Hiển thị nguyên bản theo từng chuỗi nghệ sĩ trong tệp")
}

fun extractArtistNames(rawArtist: String): List<String> = LibraryGrouping.artistNames(rawArtist)

enum class AlbumViewMode {
    LIST, GRID_2, GRID_3
}

enum class HomeSubView {
    MAIN, PLAYLISTS, ARTISTS, ALBUMS, ARTIST_DETAIL, ALBUM_DETAIL, PLAYLIST_DETAIL
}
