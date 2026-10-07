package com.example.onemusic.data.playlist

import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class M3uPlaylistManagerTest {

    private lateinit var sampleTracks: List<Track>
    private lateinit var samplePlaylist: CustomPlaylist

    @Before
    fun setUp() {
        sampleTracks = listOf(
            Track(
                id = "track_1",
                title = "Cybernetic Horizon",
                artist = "Apex Prism",
                album = "SoundLab 2026",
                durationMs = 214000L,
                audioUrl = "content://media/external/audio/media/101",
                artworkUrl = "http://example.com/art1.jpg"
            ),
            Track(
                id = "track_2",
                title = "Midnight Starlight",
                artist = "Aura Bloom",
                album = "Cosmic Dreams",
                durationMs = 185000L,
                audioUrl = "content://media/external/audio/media/102",
                artworkUrl = "http://example.com/art2.jpg"
            ),
            Track(
                id = "track_3",
                title = "Obsidian Waves",
                artist = "One UI Soundscapes",
                album = "AMOLED Vibes",
                durationMs = 300000L,
                audioUrl = "content://media/external/audio/media/103",
                artworkUrl = "http://example.com/art3.jpg"
            )
        )

        samplePlaylist = CustomPlaylist(
            id = "pl_1",
            name = "Night Drive",
            trackIds = listOf("track_1", "track_2")
        )
    }

    @Test
    fun testExportPlaylistToM3uString_generatesCorrectHeadersAndTracks() {
        val m3uContent = M3uPlaylistManager.exportPlaylistToM3uString(samplePlaylist, sampleTracks)

        assertTrue(m3uContent.startsWith("#EXTM3U"))
        assertTrue(m3uContent.contains("#PLAYLIST:Night Drive"))
        assertTrue(m3uContent.contains("#EXTINF:214,Apex Prism - Cybernetic Horizon"))
        assertTrue(m3uContent.contains("content://media/external/audio/media/101"))
        assertTrue(m3uContent.contains("#EXTINF:185,Aura Bloom - Midnight Starlight"))
        assertTrue(m3uContent.contains("content://media/external/audio/media/102"))
        // track_3 was not in samplePlaylist
        assertTrue(!m3uContent.contains("Obsidian Waves"))
    }

    @Test
    fun testExportPlaylistToM3uString_filtersOutMissingTrackIdsGracefully() {
        val playlistWithMissing = CustomPlaylist(
            id = "pl_2",
            name = "Test Missing",
            trackIds = listOf("track_1", "non_existent_id", "track_3")
        )

        val m3uContent = M3uPlaylistManager.exportPlaylistToM3uString(playlistWithMissing, sampleTracks)

        assertTrue(m3uContent.contains("#PLAYLIST:Test Missing"))
        assertTrue(m3uContent.contains("Cybernetic Horizon"))
        assertTrue(m3uContent.contains("Obsidian Waves"))
        assertTrue(!m3uContent.contains("non_existent_id"))
    }

    @Test
    fun testExportPlaylistToM3uString_emptyPlaylist() {
        val emptyPlaylist = CustomPlaylist(
            id = "pl_empty",
            name = "Empty Mix",
            trackIds = emptyList()
        )

        val m3uContent = M3uPlaylistManager.exportPlaylistToM3uString(emptyPlaylist, sampleTracks)

        assertTrue(m3uContent.startsWith("#EXTM3U"))
        assertTrue(m3uContent.contains("#PLAYLIST:Empty Mix"))
    }
}
