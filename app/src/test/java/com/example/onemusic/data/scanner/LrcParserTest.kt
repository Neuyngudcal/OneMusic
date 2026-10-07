package com.example.onemusic.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun testStandardLrcParsing() {
        val lrc = """
            [ti:Midnight Galaxy Vibe]
            [ar:Aura Bloom]
            [00:00.00] Khởi đầu giai điệu không gian
            [00:08.50] Ánh sáng neon soi sáng màn đêm đen huyền ảo
            [00:18.00] Những nhịp điệu bass vang vọng khắp không gian
            [01:05.500] One UI 8.5 mang lại trải nghiệm âm thanh đắm chìm
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(4, lines.size)

        assertEquals(0L, lines[0].timestampMs)
        assertEquals("Khởi đầu giai điệu không gian", lines[0].text)

        assertEquals(8500L, lines[1].timestampMs)
        assertEquals("Ánh sáng neon soi sáng màn đêm đen huyền ảo", lines[1].text)

        assertEquals(18000L, lines[2].timestampMs)
        assertEquals("Những nhịp điệu bass vang vọng khắp không gian", lines[2].text)

        assertEquals(65500L, lines[3].timestampMs)
        assertEquals("One UI 8.5 mang lại trải nghiệm âm thanh đắm chìm", lines[3].text)
        
        // Check that words were generated
        assertTrue(lines[0].words.isNotEmpty())
        assertEquals("Khởi", lines[0].words[0].text)
    }

    @Test
    fun testEnhancedLrcWordTimings() {
        val lrc = """
            [00:12.34] <00:12.34> When <00:12.80> the <00:13.10> night <00:13.90> has <00:14.20> come
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(1, lines.size)
        assertEquals(12340L, lines[0].timestampMs)
        assertEquals("When the night has come", lines[0].text)

        val words = lines[0].words
        assertEquals(5, words.size)
        assertEquals("When", words[0].text)
        assertEquals(12340L, words[0].startMs)
        assertEquals(12800L, words[0].endMs)

        assertEquals("the", words[1].text)
        assertEquals(12800L, words[1].startMs)
        assertEquals(13100L, words[1].endMs)

        assertEquals("night", words[2].text)
        assertEquals(13100L, words[2].startMs)
        assertEquals(13900L, words[2].endMs)

        assertEquals("has", words[3].text)
        assertEquals(13900L, words[3].startMs)
        assertEquals(14200L, words[3].endMs)

        assertEquals("come", words[4].text)
        assertEquals(14200L, words[4].startMs)
    }

    @Test
    fun testWordDurationTags() {
        val lrc = """
            [00:05.00] Hello(400) World(600)
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(1, lines.size)
        assertEquals("Hello World", lines[0].text)

        val words = lines[0].words
        assertEquals(2, words.size)
        assertEquals("Hello", words[0].text)
        assertEquals(5000L, words[0].startMs)
        assertEquals(5400L, words[0].endMs)

        assertEquals("World", words[1].text)
        assertTrue(words[1].startMs >= 5400L)
    }

    @Test
    fun testMultiTimestampLine() {
        val lrc = """
            [00:10.00][00:30.00] Repeated chorus line
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(2, lines.size)
        assertEquals(10000L, lines[0].timestampMs)
        assertEquals("Repeated chorus line", lines[0].text)
        assertEquals(30000L, lines[1].timestampMs)
        assertEquals("Repeated chorus line", lines[1].text)
    }

    @Test
    fun testOffsetAdjustment() {
        val lrc = """
            [offset:500]
            [00:10.00] Shifted by +500ms
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(1, lines.size)
        assertEquals(10500L, lines[0].timestampMs)
    }

    @Test
    fun testPlainLyricsFallback() {
        val plain = """
            Line 1 of plain lyrics
            Line 2 of plain lyrics
            Line 3 of plain lyrics
        """.trimIndent()

        val lines = LrcParser.parseLrc(plain, durationMs = 30000L)
        assertEquals(3, lines.size)
        assertEquals(0L, lines[0].timestampMs)
        assertEquals(10000L, lines[1].timestampMs)
        assertEquals(20000L, lines[2].timestampMs)
        // Plain lyrics should NOT be marked as synced or enhanced
        assertEquals(false, lines[0].isSynced)
        assertEquals(false, lines[0].isEnhanced)
    }

    @Test
    fun testEnhancedLrcPairTags() {
        val lrc = """
            [00:05.00] <00:05.00>Hello<00:05.50> <00:05.50>World<00:06.00>
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(1, lines.size)
        assertEquals(2, lines[0].words.size)
        assertEquals("Hello", lines[0].words[0].text)
        assertEquals(5000L, lines[0].words[0].startMs)
        assertEquals(5500L, lines[0].words[0].endMs)

        assertEquals("World", lines[0].words[1].text)
        assertEquals(5500L, lines[0].words[1].startMs)
        assertEquals(6000L, lines[0].words[1].endMs)
        assertEquals(true, lines[0].isEnhanced)
    }

    @Test
    fun testDualParamWordDuration() {
        val lrc = """
            [00:10.00] One(0,300) Music(300,500)
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(1, lines.size)
        assertEquals(2, lines[0].words.size)
        assertEquals("One", lines[0].words[0].text)
        assertEquals(10000L, lines[0].words[0].startMs)
        assertEquals(10300L, lines[0].words[0].endMs)

        assertEquals("Music", lines[0].words[1].text)
        assertEquals(10300L, lines[0].words[1].startMs)
        assertEquals(10800L, lines[0].words[1].endMs)
        assertEquals(true, lines[0].isEnhanced)
    }
}
