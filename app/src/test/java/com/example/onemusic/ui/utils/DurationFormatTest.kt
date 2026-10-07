package com.example.onemusic.ui.utils

import com.example.onemusic.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DurationFormatTest {

    private fun track(durationMs: Long) = Track(
        id = durationMs.toString(), title = "T", artist = "A", album = "B",
        durationMs = durationMs, audioUrl = "", artworkUrl = ""
    )

    @Test
    fun testFormatDurationUnpadded() {
        // Kiểu của Now Playing & hàng đợi
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("0:00", formatDuration(999))
        assertEquals("12:34", formatDuration(754_000))
        assertEquals("60:00", formatDuration(3_600_000))
    }

    @Test
    fun testFormatDurationPadded() {
        // Kiểu của Thư viện, Dọn trùng lặp, đồng hồ hẹn giờ tắt
        assertEquals("01:05", formatDuration(65_000, padMinutes = true))
        assertEquals("00:00", formatDuration(0, padMinutes = true))
        assertEquals("12:34", formatDuration(754_000, padMinutes = true))
        assertEquals("125:00", formatDuration(7_500_000, padMinutes = true))
    }

    @Test
    fun testFormatDurationNegativeIsZero() {
        assertEquals("0:00", formatDuration(-5_000))
        assertEquals("00:00", formatDuration(-1, padMinutes = true))
    }

    @Test
    fun testFormatDurationAlwaysUsesAsciiDigits() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-u-nu-arab"))
            assertEquals("1:05", formatDuration(65_000))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun testFormatRemaining() {
        assertEquals("-3:05", formatRemaining(currentMs = 60_000, totalMs = 245_000))
        assertEquals("-0:00", formatRemaining(currentMs = 300_000, totalMs = 200_000))
        assertEquals("--:--", formatRemaining(currentMs = 0, totalMs = 0))
    }

    @Test
    fun testFormatTotalDuration() {
        assertEquals("0 phút", formatTotalDuration(emptyList()))
        assertEquals("59 phút", formatTotalDuration(listOf(track(3_599_000))))
        assertEquals("1 giờ 0 phút", formatTotalDuration(listOf(track(1_800_000), track(1_800_000))))
        assertEquals("1 giờ 5 phút", formatTotalDuration(listOf(track(3_900_000))))
    }
}
