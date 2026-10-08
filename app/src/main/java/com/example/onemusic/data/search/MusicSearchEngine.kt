package com.example.onemusic.data.search

import com.example.onemusic.data.model.Track

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
