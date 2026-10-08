package com.example.onemusic.data.search

import com.example.onemusic.data.model.Track

enum class SearchMatchReason {
    EXACT_ARTIST,
    ARTIST,
    EXACT_TITLE,
    TITLE,
    CROSS_FIELD,
    ALBUM,
    LYRICS,
    FUZZY
}

data class MatchedTrack(
    val track: Track,
    val score: Int,
    val matchedLyricSnippet: String? = null,
    val matchedArtistName: String? = null,
    val reason: SearchMatchReason = SearchMatchReason.TITLE
)

data class MatchedArtist(
    val artistName: String,
    val trackCount: Int,
    val representativeTrack: Track,
    val score: Int
)

data class MatchedAlbum(
    /** Khóa album (LibraryGrouping.albumKey) – dùng để mở trang chi tiết, vì tên album có thể trùng */
    val albumKey: String,
    val albumName: String,
    val artistName: String,
    val trackCount: Int,
    val representativeTrack: Track,
    val score: Int
)

data class SearchResults(
    val tracks: List<MatchedTrack> = emptyList(),
    val artists: List<MatchedArtist> = emptyList(),
    val albums: List<MatchedAlbum> = emptyList()
)

data class SearchableTrack(
    val track: Track,
    val normTitle: String,
    val normArtist: String,
    val normAlbum: String,
    val individualArtists: List<String>,
    val normIndividualArtists: List<String>,
    val titleTokens: List<String>,
    val artistTokens: List<String>,
    val albumTokens: List<String>,
    val combinedTokens: List<String>,
    val normLyrics: List<Pair<String, String>> = emptyList()
)
