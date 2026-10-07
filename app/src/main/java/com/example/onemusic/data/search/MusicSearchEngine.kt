package com.example.onemusic.data.search

import com.example.onemusic.data.model.Track
import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

object SearchTextNormalizer {

    private val COMBINING_DIACRITICS_REGEX = Regex("""\p{M}+""")
    private val PUNCTUATION_AND_SPECIALS = Regex("""[^\p{L}\p{Nd}\s]""")
    private val MULTIPLE_SPACES = Regex("""\s+""")

    /**
     * Normalizes text by:
     * 1. Converting to lowercase
     * 2. Converting Vietnamese specific letters (đ/Đ -> d)
     * 3. Stripping all accents and diacritical marks (NFD)
     * 4. Removing special characters/punctuations (&, -, +, etc.)
     * 5. Collapsing consecutive whitespaces
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""

        // Step 1 & 2: Lowercase and replace Vietnamese specific 'đ' / 'Đ'
        val lower = input.lowercase(Locale.ROOT)
            .replace('đ', 'd')
            .replace('Đ', 'd')

        // Step 3: NFD decomposition to remove accents
        val nfd = Normalizer.normalize(lower, Normalizer.Form.NFD)
        val withoutAccents = COMBINING_DIACRITICS_REGEX.replace(nfd, "")

        // Step 4: Remove punctuation, separators (&, +, -, /, etc.)
        val cleanChars = PUNCTUATION_AND_SPECIALS.replace(withoutAccents, " ")

        // Step 5: Trim and collapse whitespace
        return MULTIPLE_SPACES.replace(cleanChars, " ").trim()
    }

    /**
     * Splits normalized text into individual word tokens.
     */
    fun tokenize(input: String?): List<String> {
        val normalized = normalize(input)
        if (normalized.isBlank()) return emptyList()
        return normalized.split(' ').filter { it.isNotBlank() }
    }
}

object ArtistExtractor {

    private val FEAT_PAREN_REGEX = Regex("(?i)\\s*\\((?:feat\\.|ft\\.|featuring)\\s+[^)]+\\)")
    private val FEAT_BRACKET_REGEX = Regex("(?i)\\[(?:feat\\.|ft\\.|featuring)\\s+[^]]+\\]")

    // Strict multi-artist delimiter: symbols like comma/semicolon/slash OR whole words with surrounding whitespace
    private val ARTIST_SPLIT_REGEX = Regex(
        """\s*[,;/]\s*|\s+(?:&|\+|feat\.?|ft\.?|featuring|with|vs\.?|[xX])\s+""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Splits compound artist strings like "Aura Bloom & Samsung SoundLab"
     * into separate individual artist names: ["Aura Bloom", "Samsung SoundLab"].
     * Safely preserves single artists with 'x' in their names (e.g. "Charli xcx", "Lil Nas X", "The xx").
     */
    fun extractIndividualArtists(rawArtist: String?): List<String> {
        if (rawArtist.isNullOrBlank()) return emptyList()

        var cleaned = rawArtist.trim()
        cleaned = FEAT_PAREN_REGEX.replace(cleaned, "")
        cleaned = FEAT_BRACKET_REGEX.replace(cleaned, "")

        // Remove specific trailing parenthesized metadata tags like (Remix), (Live), (Official), etc.
        if (cleaned.endsWith(")") && cleaned.contains("(")) {
            val insideParen = cleaned.substringAfterLast("(").substringBefore(")")
            if (insideParen.length <= 6 ||
                insideParen.contains("MO", ignoreCase = true) ||
                insideParen.contains("Live", ignoreCase = true) ||
                insideParen.contains("Remix", ignoreCase = true) ||
                insideParen.contains("Audio", ignoreCase = true) ||
                insideParen.contains("Official", ignoreCase = true)
            ) {
                cleaned = cleaned.substringBeforeLast("(").trim()
            }
        }

        val parts = cleaned
            .split(ARTIST_SPLIT_REGEX)
            .map { it.trim().trim(';', '/', ',', '&', '-', '(', ')', '[', ']', '"', '\'') }
            .filter {
                it.isNotBlank() &&
                it.length > 1 &&
                !it.equals("Unknown", ignoreCase = true) &&
                !it.contains("chưa rõ", ignoreCase = true)
            }
            .distinct()

        return if (parts.isEmpty()) {
            val fallback = rawArtist.trim()
            if (fallback.isNotBlank() && !fallback.equals("Unknown", ignoreCase = true) && !fallback.contains("chưa rõ", ignoreCase = true)) {
                listOf(fallback)
            } else {
                emptyList()
            }
        } else {
            parts
        }
    }

    /**
     * Resolves and merges prefix aliases into full canonical artist names.
     * E.g. If both "Charli" and "Charli xcx" exist, merges all tracks of "Charli" into "Charli xcx".
     */
    fun mergeCanonicalArtists(rawMap: Map<String, List<Track>>): Map<String, List<Track>> {
        if (rawMap.isEmpty()) return emptyMap()

        val canonicalMap = mutableMapOf<String, MutableList<Track>>()
        // Sort keys by length DESC so full names (e.g. "Charli xcx") are evaluated first
        val sortedKeys = rawMap.keys.sortedByDescending { it.length }
        val aliasMap = mutableMapOf<String, String>()

        for (key in sortedKeys) {
            // Find if this key is a prefix alias of an already registered longer artist name
            val targetCanonical = aliasMap[key] ?: sortedKeys.firstOrNull { longerKey ->
                longerKey.length > key.length &&
                (longerKey.startsWith("$key ", ignoreCase = true) ||
                 longerKey.startsWith("${key}x", ignoreCase = true) ||
                 longerKey.startsWith("${key}_", ignoreCase = true))
            }

            if (targetCanonical != null && targetCanonical != key) {
                aliasMap[key] = targetCanonical
                canonicalMap.getOrPut(targetCanonical) { mutableListOf() }.addAll(rawMap[key].orEmpty())
            } else {
                canonicalMap.getOrPut(key) { mutableListOf() }.addAll(rawMap[key].orEmpty())
            }
        }

        return canonicalMap.mapValues { (_, tracks) -> tracks.distinctBy { it.id } }
    }
}

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

object MusicSearchEngine {

    /**
     * Performs an ultra-fast, intelligent, Vietnamese-aware, diacritics-stripping,
     * tokenized, fuzzy, and cross-field music search using a pre-computed SearchIndex.
     */
    fun search(index: MusicSearchIndex, rawQuery: String): SearchResults {
        val query = rawQuery.trim()
        if (query.isBlank() || index.searchableTracks.isEmpty()) {
            return SearchResults()
        }

        val normQuery = SearchTextNormalizer.normalize(query)
        val queryTokens = SearchTextNormalizer.tokenize(query)
        if (normQuery.isBlank() || queryTokens.isEmpty()) {
            return SearchResults()
        }

        val matchedTracks = mutableListOf<MatchedTrack>()
        val matchedArtistsMap = mutableMapOf<String, Int>()
        val matchedAlbumsMap = mutableMapOf<String, Int>()

        for (item in index.searchableTracks) {
            val track = item.track
            val normTitle = item.normTitle
            val normArtist = item.normArtist
            val normAlbum = item.normAlbum
            val normIndividualArtists = item.normIndividualArtists

            var score = 0
            var matchedReason = SearchMatchReason.TITLE
            var matchedLyricSnippet: String? = null
            var matchedArtistName: String? = null

            // 1. EXACT ARTIST MATCH
            if (normArtist == normQuery) {
                score = maxOf(score, 1200)
                matchedReason = SearchMatchReason.EXACT_ARTIST
                matchedArtistName = track.artist
            } else {
                val exactSubArtist = normIndividualArtists.indexOfFirst { it == normQuery }
                if (exactSubArtist != -1) {
                    score = maxOf(score, 1100)
                    matchedReason = SearchMatchReason.EXACT_ARTIST
                    matchedArtistName = item.individualArtists[exactSubArtist]
                }
            }

            // 2. EXACT TITLE MATCH
            if (normTitle == normQuery) {
                score = maxOf(score, 1050)
                matchedReason = SearchMatchReason.EXACT_TITLE
            }

            // 3. ARTIST PREFIX / SUBSTRING MATCH
            if (score < 1000) {
                if (normArtist.startsWith(normQuery)) {
                    score = maxOf(score, 900)
                    matchedReason = SearchMatchReason.ARTIST
                    matchedArtistName = track.artist
                } else if (normIndividualArtists.any { it.startsWith(normQuery) }) {
                    val idx = normIndividualArtists.indexOfFirst { it.startsWith(normQuery) }
                    score = maxOf(score, 850)
                    matchedReason = SearchMatchReason.ARTIST
                    matchedArtistName = item.individualArtists.getOrNull(idx) ?: track.artist
                } else if (normArtist.contains(normQuery)) {
                    score = maxOf(score, 780)
                    matchedReason = SearchMatchReason.ARTIST
                    matchedArtistName = track.artist
                }
            }

            // 4. TITLE PREFIX / SUBSTRING MATCH
            if (score < 900) {
                if (normTitle.startsWith(normQuery)) {
                    score = maxOf(score, 820)
                    if (matchedReason != SearchMatchReason.EXACT_ARTIST) matchedReason = SearchMatchReason.TITLE
                } else if (normTitle.contains(normQuery)) {
                    score = maxOf(score, 740)
                    if (matchedReason != SearchMatchReason.EXACT_ARTIST) matchedReason = SearchMatchReason.TITLE
                }
            }

            // 5. MULTI-TOKEN / CROSS-FIELD MATCHING
            if (queryTokens.size > 1) {
                val allInArtist = queryTokens.all { q -> item.artistTokens.any { a -> a.startsWith(q) || a.contains(q) } }
                val allInTitle = queryTokens.all { q -> item.titleTokens.any { t -> t.startsWith(q) || t.contains(q) } }
                val allInCombined = queryTokens.all { q -> item.combinedTokens.any { c -> c.startsWith(q) || c.contains(q) } }

                if (allInArtist) {
                    score = maxOf(score, 760)
                    matchedReason = SearchMatchReason.ARTIST
                    matchedArtistName = track.artist
                } else if (allInTitle) {
                    score = maxOf(score, 720)
                    if (matchedReason != SearchMatchReason.EXACT_ARTIST) matchedReason = SearchMatchReason.TITLE
                } else if (allInCombined) {
                    score = maxOf(score, 620)
                    if (matchedReason != SearchMatchReason.EXACT_ARTIST) matchedReason = SearchMatchReason.CROSS_FIELD
                }
            }

            // 6. ALBUM MATCH
            if (normAlbum.contains(normQuery) || (queryTokens.size > 1 && queryTokens.all { normAlbum.contains(it) })) {
                score = maxOf(score, 450)
                if (matchedReason == SearchMatchReason.TITLE && !normTitle.contains(normQuery)) {
                    matchedReason = SearchMatchReason.ALBUM
                }
            }

            // 7. LYRICS MATCH
            if (item.normLyrics.isNotEmpty()) {
                for ((origText, normLyric) in item.normLyrics) {
                    if (normLyric.contains(normQuery) || (queryTokens.size > 1 && queryTokens.all { normLyric.contains(it) })) {
                        score = maxOf(score, 380)
                        matchedLyricSnippet = origText.trim()
                        if (score < 500) {
                            matchedReason = SearchMatchReason.LYRICS
                        }
                        break
                    }
                }
            }

            // 8. FAST FUZZY MATCHING (Only if no high match and query is >= 3 chars)
            if (score == 0 && normQuery.length >= 3) {
                var fuzzyMatched = false
                for (q in queryTokens) {
                    if (q.length >= 3) {
                        val artistFuzzy = item.artistTokens.any { a -> isFuzzyMatch(q, a) }
                        val titleFuzzy = item.titleTokens.any { t -> isFuzzyMatch(q, t) }

                        if (artistFuzzy || titleFuzzy) {
                            fuzzyMatched = true
                            score = maxOf(score, if (artistFuzzy) 280 else 240)
                            matchedReason = SearchMatchReason.FUZZY
                            if (artistFuzzy) matchedArtistName = track.artist
                        }
                    }
                }

                if (!fuzzyMatched && normQuery.length >= 4) {
                    if (isFuzzyMatch(normQuery, normArtist)) {
                        score = maxOf(score, 300)
                        matchedReason = SearchMatchReason.FUZZY
                        matchedArtistName = track.artist
                    } else if (isFuzzyMatch(normQuery, normTitle)) {
                        score = maxOf(score, 260)
                        matchedReason = SearchMatchReason.FUZZY
                    }
                }
            }

            if (score > 0) {
                matchedTracks.add(
                    MatchedTrack(
                        track = track,
                        score = score,
                        matchedLyricSnippet = matchedLyricSnippet,
                        matchedArtistName = matchedArtistName,
                        reason = matchedReason
                    )
                )
            }
        }

        // Sort matched tracks by score DESC, then title ASC
        val sortedTracks = matchedTracks.sortedWith(
            compareByDescending<MatchedTrack> { it.score }
                .thenBy { it.track.title }
        )

        // Process Matched Artists from index
        val matchedArtists = mutableListOf<MatchedArtist>()
        for ((artistName, artistTrackList) in index.artistTracksMap) {
            val normArt = SearchTextNormalizer.normalize(artistName)
            var artScore = 0

            if (normArt == normQuery) {
                artScore = 1200
            } else if (normArt.startsWith(normQuery)) {
                artScore = 900
            } else if (normArt.contains(normQuery)) {
                artScore = 750
            } else if (queryTokens.size > 1 && queryTokens.all { normArt.contains(it) }) {
                artScore = 700
            } else if (normQuery.length >= 3 && isFuzzyMatch(normQuery, normArt)) {
                artScore = 300
            }

            if (artScore > 0 && artistTrackList.isNotEmpty()) {
                matchedArtists.add(
                    MatchedArtist(
                        artistName = artistName,
                        trackCount = artistTrackList.size,
                        representativeTrack = artistTrackList.first(),
                        score = artScore
                    )
                )
            }
        }
        val sortedArtists = matchedArtists.sortedByDescending { it.score }

        // Process Matched Albums from index
        val matchedAlbums = mutableListOf<MatchedAlbum>()
        for ((albumKey, albumTrackList) in index.albumTracksMap) {
            if (albumTrackList.isEmpty()) continue
            val repTrack = albumTrackList.first()
            val albumName = LibraryGrouping.albumName(repTrack)
            val normAlb = SearchTextNormalizer.normalize(albumName)
            var albScore = 0

            if (normAlb == normQuery) {
                albScore = 1000
            } else if (normAlb.startsWith(normQuery)) {
                albScore = 800
            } else if (normAlb.contains(normQuery)) {
                albScore = 650
            } else if (queryTokens.size > 1 && queryTokens.all { normAlb.contains(it) }) {
                albScore = 600
            }

            if (albScore > 0) {
                matchedAlbums.add(
                    MatchedAlbum(
                        albumKey = albumKey,
                        albumName = albumName,
                        artistName = LibraryGrouping.artistNames(repTrack.artist).first(),
                        trackCount = albumTrackList.size,
                        representativeTrack = repTrack,
                        score = albScore
                    )
                )
            }
        }
        val sortedAlbums = matchedAlbums.sortedByDescending { it.score }

        return SearchResults(
            tracks = sortedTracks,
            artists = sortedArtists,
            albums = sortedAlbums
        )
    }

    /**
     * Fallback search for simple lists of tracks.
     */
    fun search(tracks: List<Track>, rawQuery: String): SearchResults {
        val index = MusicSearchIndex.build(tracks)
        return search(index, rawQuery)
    }

    /**
     * High-speed Levenshtein distance check with 2 1D arrays and early exit.
     */
    private fun isFuzzyMatch(s1: String, s2: String): Boolean {
        if (s1 == s2) return true
        val maxDist = when {
            s1.length <= 3 || s2.length <= 3 -> 0
            s1.length <= 6 || s2.length <= 6 -> 1
            else -> 2
        }
        if (kotlin.math.abs(s1.length - s2.length) > maxDist) return false
        return computeLevenshteinDistance(s1, s2, maxDist) <= maxDist
    }

    private fun computeLevenshteinDistance(s1: String, s2: String, maxLimit: Int): Int {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0) return len2
        if (len2 == 0) return len1
        if (kotlin.math.abs(len1 - len2) > maxLimit) return maxLimit + 1

        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)

        for (i in 1..len1) {
            curr[0] = i
            var minInRow = curr[0]
            val c1 = s1[i - 1]
            for (j in 1..len2) {
                val cost = if (c1 == s2[j - 1]) 0 else 1
                val insertion = curr[j - 1] + 1
                val deletion = prev[j] + 1
                val substitution = prev[j - 1] + cost
                val minVal = minOf(insertion, deletion, substitution)
                curr[j] = minVal
                if (minVal < minInRow) minInRow = minVal
            }
            if (minInRow > maxLimit) return maxLimit + 1
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[len2]
    }
}
