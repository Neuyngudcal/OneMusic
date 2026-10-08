package com.example.onemusic.data.scanner.motion

import com.example.onemusic.data.model.Track
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Các phần thuần (không gọi mạng) của quy trình tải bìa động Apple Music. */
class MotionFetcherHelpersTest {

    private fun track(title: String, artist: String, album: String, durationMs: Long = 200_000L) =
        Track(id = "t", title = title, artist = artist, album = album, durationMs = durationMs, audioUrl = "", artworkUrl = "")

    // ---------- StringSimilarity ----------

    @Test
    fun levenshtein_identicalAndEmpty() {
        assertEquals(1.0f, StringSimilarity.levenshteinSimilarity("abc", "abc"))
        assertEquals(0.0f, StringSimilarity.levenshteinSimilarity("", "abc"))
        assertEquals(0, StringSimilarity.levenshteinDistance("", ""))
    }

    @Test
    fun levenshtein_classicExample() {
        assertEquals(3, StringSimilarity.levenshteinDistance("kitten", "sitting"))
        assertEquals(1.0f - 3f / 7f, StringSimilarity.levenshteinSimilarity("kitten", "sitting"), 0.0001f)
    }

    // ---------- MotionHttpClient.resolveUrl ----------

    @Test
    fun resolveUrl_relativeAndAbsolute() {
        assertEquals("https://a.com/p/seg1.mp4", MotionHttpClient.resolveUrl("https://a.com/p/master.m3u8", "seg1.mp4"))
        assertEquals("https://b.com/x.mp4", MotionHttpClient.resolveUrl("https://a.com/p/master.m3u8", "https://b.com/x.mp4"))
        assertEquals("seg.mp4", MotionHttpClient.resolveUrl("master.m3u8", "seg.mp4"))
    }

    // ---------- HlsVideoDownloader ----------

    @Test
    fun selectBestVariant_prefersAvcUpTo1080p() {
        val master = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=9000000,RESOLUTION=2160x2160,CODECS="avc1.640033"
            v2160.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1080x1080,CODECS="avc1.640028"
            v1080.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1080x1080,CODECS="hvc1.2.4.L123"
            h1080.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=720x720,CODECS="avc1.64001f"
            v720.m3u8
        """.trimIndent()

        assertEquals("https://cdn/x/v1080.m3u8", HlsVideoDownloader.selectBestVariant(master, "https://cdn/x/master.m3u8"))
    }

    @Test
    fun selectBestVariant_onlyAbove1080p_picksHighest() {
        val master = """
            #EXT-X-STREAM-INF:BANDWIDTH=9000000,RESOLUTION=2160x2160,CODECS="avc1.640033"
            v2160.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=8000000,RESOLUTION=1440x1440,CODECS="avc1.640032"
            v1440.m3u8
        """.trimIndent()

        assertEquals("https://cdn/x/v2160.m3u8", HlsVideoDownloader.selectBestVariant(master, "https://cdn/x/master.m3u8"))
    }

    @Test
    fun selectBestVariant_noVariants_returnsNull() {
        assertNull(HlsVideoDownloader.selectBestVariant("#EXTM3U\n#EXT-X-VERSION:7", "https://cdn/x/master.m3u8"))
    }

    @Test
    fun findDirectMp4Url_readsExtXMap() {
        val variant = "#EXTM3U\n#EXT-X-MAP:URI=\"video.mp4\",BYTERANGE=\"800@0\"\n#EXTINF:6.0,\nvideo.mp4"

        assertEquals("https://cdn/x/video.mp4", HlsVideoDownloader.findDirectMp4Url(variant, "https://cdn/x/v1080.m3u8"))
        assertNull(HlsVideoDownloader.findDirectMp4Url("#EXTM3U\nseg1.ts", "https://cdn/x/v1080.m3u8"))
    }

    // ---------- AppleMusicAmpClient.parseEditorialVideo ----------

    @Test
    fun parseEditorialVideo_prefersSquare() {
        val json = """
            {"data":[{"attributes":{"editorialVideo":{
              "motionDetailTall":{"video":"https://mvod.itunes.apple.com/tall.m3u8"},
              "motionSquareVideo1x1":{"video":"https://mvod.itunes.apple.com/square.m3u8"}
            }}}]}
        """.trimIndent()

        val result = AppleMusicAmpClient.parseEditorialVideo(json)!!

        assertEquals("https://mvod.itunes.apple.com/square.m3u8", result.preferredVideoUrl)
        assertEquals("https://mvod.itunes.apple.com/square.m3u8", result.squareUrl)
        assertEquals("https://mvod.itunes.apple.com/tall.m3u8", result.tallUrl)
        assertTrue(!result.isTall)
    }

    @Test
    fun parseEditorialVideo_tallOnly_andIgnoresNonAppleHosts() {
        val json = """
            {"data":[{"attributes":{"editorialVideo":{
              "motionDetailSquare":{"video":"https://example.com/square.m3u8"},
              "motionTallVideo3x4":{"video":"https://mvod.itunes.apple.com/tall.m3u8"}
            }}}]}
        """.trimIndent()

        val result = AppleMusicAmpClient.parseEditorialVideo(json)!!

        assertEquals("https://mvod.itunes.apple.com/tall.m3u8", result.preferredVideoUrl)
        assertEquals("", result.squareUrl)
        assertTrue(result.isTall)
    }

    @Test
    fun parseEditorialVideo_missingOrInvalid_returnsNull() {
        assertNull(AppleMusicAmpClient.parseEditorialVideo("""{"data":[{"attributes":{}}]}"""))
        assertNull(AppleMusicAmpClient.parseEditorialVideo("not json"))
    }

    // ---------- ItunesSearchClient ----------

    @Test
    fun sanitizeQuery_stripsFeatAndEditionTags() {
        assertEquals("Song", ItunesSearchClient.sanitizeQuery("Song (feat. Someone)"))
        assertEquals("Album", ItunesSearchClient.sanitizeQuery("Album (Deluxe Edition)"))
        assertEquals("Hit", ItunesSearchClient.sanitizeQuery("Hit - Single"))
        assertEquals("Track", ItunesSearchClient.sanitizeQuery("  Track [Explicit]  "))
    }

    @Test
    fun pickBestCollectionId_choosesBestMatchAboveThreshold() {
        val results = Json.parseToJsonElement(
            """
            {"results":[
              {"trackName":"Other Song","artistName":"Someone Else","collectionName":"X","collectionId":111,"trackTimeMillis":100000},
              {"trackName":"Blinding Lights","artistName":"The Weeknd","collectionName":"After Hours","collectionId":222,"trackTimeMillis":200040},
              {"trackName":"Blinding Lights (Remix)","artistName":"The Weeknd","collectionName":"Remixes","collectionId":333,"trackTimeMillis":250000}
            ]}
            """.trimIndent()
        ).jsonObject["results"]!!.jsonArray
        val candidates = mutableListOf<String>()

        val best = ItunesSearchClient.pickBestCollectionId(
            results, track("Blinding Lights", "The Weeknd", "After Hours")
        ) { candidates += it }

        assertEquals("222", best)
        assertTrue(candidates.isNotEmpty())
    }

    @Test
    fun pickBestCollectionId_noConfidentMatch_returnsNull() {
        val results = Json.parseToJsonElement(
            """{"results":[{"trackName":"Totally Different","artistName":"Nobody","collectionName":"Y","collectionId":9}]}"""
        ).jsonObject["results"]!!.jsonArray

        assertNull(ItunesSearchClient.pickBestCollectionId(results, track("Blinding Lights", "The Weeknd", "After Hours")))
    }
}
