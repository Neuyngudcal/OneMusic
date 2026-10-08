package com.example.onemusic.data.search

import com.example.onemusic.data.model.Track

class MusicSearchIndex(
    val searchableTracks: List<SearchableTrack>,
    val allArtists: List<MatchedArtist>,
    val allAlbums: List<MatchedAlbum>,
    val artistTracksMap: Map<String, List<Track>>,
    /** Khóa album (LibraryGrouping.albumKey) → các bài của album */
    val albumTracksMap: Map<String, List<Track>>
) {
    companion object {
        fun build(tracks: List<Track>): MusicSearchIndex {
            val searchableList = ArrayList<SearchableTrack>(tracks.size)

            for (track in tracks) {
                val normTitle = SearchTextNormalizer.normalize(track.title)
                val normArtist = SearchTextNormalizer.normalize(track.artist)
                val normAlbum = SearchTextNormalizer.normalize(track.album)

                val individualArtists = ArtistExtractor.extractIndividualArtists(track.artist)
                val normIndividualArtists = individualArtists.map { SearchTextNormalizer.normalize(it) }

                val titleTokens = if (normTitle.isNotBlank()) normTitle.split(' ').filter { it.isNotBlank() } else emptyList()
                val artistTokens = if (normArtist.isNotBlank()) normArtist.split(' ').filter { it.isNotBlank() } else emptyList()
                val albumTokens = if (normAlbum.isNotBlank()) normAlbum.split(' ').filter { it.isNotBlank() } else emptyList()
                val combinedTokens = titleTokens + artistTokens + albumTokens

                val normLyrics = if (track.lyrics.isNotEmpty()) {
                    track.lyrics.map { Pair(it.text, SearchTextNormalizer.normalize(it.text)) }
                } else {
                    emptyList()
                }

                searchableList.add(
                    SearchableTrack(
                        track = track,
                        normTitle = normTitle,
                        normArtist = normArtist,
                        normAlbum = normAlbum,
                        individualArtists = individualArtists,
                        normIndividualArtists = normIndividualArtists,
                        titleTokens = titleTokens,
                        artistTokens = artistTokens,
                        albumTokens = albumTokens,
                        combinedTokens = combinedTokens,
                        normLyrics = normLyrics
                    )
                )
            }

            // Gộp nghệ sĩ/album bằng hàm dùng chung với Trang chủ & Thư viện (Mục 7e).
            // Riêng Tìm kiếm bỏ các nhóm "chưa rõ" và album tạo từ tên thư mục – không phải kết quả hữu ích.
            val artistMap: Map<String, List<Track>> = LibraryGrouping.groupArtists(tracks)
                .filter { (name, _) -> name != LibraryGrouping.UNKNOWN_ARTIST }
                .toMap()
            val albumGroups = LibraryGrouping.groupAlbums(tracks)
                .filter { it.name != LibraryGrouping.UNKNOWN_ALBUM && !it.name.contains("Thư mục", ignoreCase = true) }
            val albumMap: Map<String, List<Track>> = albumGroups.associate { it.key to it.tracks }

            val artistsList = artistMap.map { (name, list) ->
                MatchedArtist(name, list.size, list.first(), 0)
            }.sortedByDescending { it.trackCount }

            val albumsList = albumGroups.map { album ->
                MatchedAlbum(album.key, album.name, album.artist, album.tracks.size, album.tracks.first(), 0)
            } // giữ thứ tự A→Z của groupAlbums (giống Trang chủ & Thư viện)

            return MusicSearchIndex(
                searchableTracks = searchableList,
                allArtists = artistsList,
                allAlbums = albumsList,
                artistTracksMap = artistMap,
                albumTracksMap = albumMap
            )
        }
    }
}
