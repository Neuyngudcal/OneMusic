package com.example.onemusic.data.search

import com.example.onemusic.data.model.LyricLine
import com.example.onemusic.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MusicSearchEngineTest {

    private lateinit var sampleTracks: List<Track>

    @Before
    fun setUp() {
        sampleTracks = listOf(
            Track(
                id = "track_1",
                title = "Midnight Galaxy Vibe",
                artist = "Aura Bloom & Samsung SoundLab",
                album = "One UI Horizons 8.5",
                durationMs = 214000,
                audioUrl = "http://example.com/1.mp3",
                artworkUrl = "http://example.com/1.jpg",
                lyrics = listOf(
                    LyricLine(0, "♪ Khởi đầu giai điệu không gian ♪"),
                    LyricLine(8000, "Ánh sáng neon soi sáng màn đêm đen huyền ảo"),
                    LyricLine(18000, "Những nhịp điệu bass vang vọng khắp không gian")
                )
            ),
            Track(
                id = "track_2",
                title = "Echoes of Starlight",
                artist = "Luna Eclipse",
                album = "Cosmic Dreams",
                durationMs = 188000,
                audioUrl = "http://example.com/2.mp3",
                artworkUrl = "http://example.com/2.jpg",
                lyrics = listOf(
                    LyricLine(0, "♪ Tiếng đàn ngân trong đêm vô tận ♪"),
                    LyricLine(12000, "Ngôi sao xa xôi gửi lời thì thầm")
                )
            ),
            Track(
                id = "track_3",
                title = "Seoul Neon Drive",
                artist = "K-Wave Collective",
                album = "Night City Pulse",
                durationMs = 245000,
                audioUrl = "http://example.com/3.mp3",
                artworkUrl = "http://example.com/3.jpg"
            ),
            Track(
                id = "track_4",
                title = "Acoustic Morning Breeze",
                artist = "Minh Triết & Strings",
                album = "Peaceful Horizons",
                durationMs = 175000,
                audioUrl = "http://example.com/4.mp3",
                artworkUrl = "http://example.com/4.jpg",
                lyrics = listOf(
                    LyricLine(0, "♪ Tiếng guitar mộc mạc buổi sớm ♪"),
                    LyricLine(10000, "Tia nắng đầu tiên đánh thức ngày mới")
                )
            ),
            Track(
                id = "track_5",
                title = "Trốn Tìm",
                artist = "Đen Vâu feat. MTV Band",
                album = "Show Của Đen",
                durationMs = 260000,
                audioUrl = "http://example.com/5.mp3",
                artworkUrl = "http://example.com/5.jpg"
            )
        )
    }

    @Test
    fun testSearchVietnameseArtistWithoutAccents() {
        // User searches "minh triet" without accents
        val results = MusicSearchEngine.search(sampleTracks, "minh triet")
        assertTrue("Should find track with 'Minh Triết'", results.tracks.isNotEmpty())
        assertEquals("track_4", results.tracks.first().track.id)
        assertTrue(results.artists.any { it.artistName.contains("Minh Triết") })
    }

    @Test
    fun testSearchVietnameseDLetter() {
        // User searches "den vau" without 'đ'/'Đ'
        val results = MusicSearchEngine.search(sampleTracks, "den vau")
        assertTrue("Should find 'Đen Vâu'", results.tracks.isNotEmpty())
        assertEquals("track_5", results.tracks.first().track.id)
        assertTrue(results.artists.any { it.artistName.contains("Đen Vâu") })
    }

    @Test
    fun testSearchCompoundArtistWithoutSpecialCharacters() {
        // User searches "Aura Bloom Samsung SoundLab" without '&'
        val results = MusicSearchEngine.search(sampleTracks, "Aura Bloom Samsung SoundLab")
        assertTrue("Should find 'Aura Bloom & Samsung SoundLab'", results.tracks.isNotEmpty())
        assertEquals("track_1", results.tracks.first().track.id)
    }

    @Test
    fun testSearchSubArtistIndividual() {
        // User searches for individual sub-artist "Samsung SoundLab" or "Strings" or "MTV Band"
        val resultsSoundLab = MusicSearchEngine.search(sampleTracks, "soundlab")
        assertTrue(resultsSoundLab.tracks.any { it.track.id == "track_1" })
        assertTrue(resultsSoundLab.artists.any { it.artistName == "Samsung SoundLab" })

        val resultsStrings = MusicSearchEngine.search(sampleTracks, "strings")
        assertTrue(resultsStrings.tracks.any { it.track.id == "track_4" })

        val resultsMtv = MusicSearchEngine.search(sampleTracks, "mtv")
        assertTrue(resultsMtv.tracks.any { it.track.id == "track_5" })
    }

    @Test
    fun testSearchCrossFieldTitleAndArtist() {
        // User searches "triet acoustic" (Artist + Title words)
        val results = MusicSearchEngine.search(sampleTracks, "triet acoustic")
        assertTrue("Should find cross-field match", results.tracks.isNotEmpty())
        assertEquals("track_4", results.tracks.first().track.id)
    }

    @Test
    fun testSearchInLyrics() {
        // User searches "buoi som" from lyric line "Tiếng guitar mộc mạc buổi sớm"
        val results = MusicSearchEngine.search(sampleTracks, "buoi som")
        assertTrue("Should find track by lyrics", results.tracks.isNotEmpty())
        val matchedTrack = results.tracks.first()
        assertEquals("track_4", matchedTrack.track.id)
        assertNotNull(matchedTrack.matchedLyricSnippet)
        assertTrue(matchedTrack.matchedLyricSnippet!!.contains("buổi sớm"))
    }

    @Test
    fun testSearchFuzzyTypo() {
        // User has a small typo: "samung" instead of "samsung"
        val results = MusicSearchEngine.search(sampleTracks, "samung")
        assertTrue("Should tolerate typo 'samung'", results.tracks.isNotEmpty())
        assertEquals("track_1", results.tracks.first().track.id)
    }

    @Test
    fun testSearchHyphenatedArtist() {
        // User searches "k wave" or "kwave"
        val results = MusicSearchEngine.search(sampleTracks, "k wave")
        assertTrue("Should find 'K-Wave Collective'", results.tracks.isNotEmpty())
        assertEquals("track_3", results.tracks.first().track.id)
    }
}
