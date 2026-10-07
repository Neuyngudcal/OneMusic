package com.example.onemusic.data.dedup

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.example.onemusic.data.model.Track
import java.io.File
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.max

@Immutable
data class DuplicateGroup(
    val groupKey: String,
    val title: String,
    val artist: String,
    val primaryTrack: Track, // Bản nhạc chất lượng cao nhất được khuyên giữ lại
    val duplicateTracks: List<Track>, // Các bản nhạc trùng lặp đề xuất xử lý
    val totalReclaimableBytes: Long = 0L
)


object DuplicateAudioDetector {

    // Danh sách từ khóa phiên bản đặc biệt - Nếu khác nhau thì TUYỆT ĐỐI KHÔNG COI LÀ TRÙNG LẶP
    private val VERSION_KEYWORDS = setOf(
        "live", "remix", "acoustic", "instrumental", "karaoke", "slowed", "sped up",
        "speed up", "extended", "radio edit", "orchestral", "unplugged", "demo",
        "acapella", "a capella", "nightcore", "daycore", "piano version", "guitar version",
        "club mix", "clean version", "explicit version", "cover", "reverb", "mashup"
    )

    // Tiêu đề chung trong các album - Bắt buộc phải trùng cả Album và Duration mới xét
    private val GENERIC_TITLES = setOf(
        "intro", "outro", "interlude", "prelude", "prologue", "epilogue",
        "track 1", "track 01", "track 2", "track 02", "track 3", "track 03",
        "theme", "untitled", "skit", "opening", "ending", "bonus track"
    )

    /**
     * Quét và gom nhóm các bài hát trùng lặp an toàn
     */
    fun findDuplicates(context: Context, tracks: List<Track>): List<DuplicateGroup> {
        if (tracks.size < 2) return emptyList()

        val groups = mutableListOf<MutableList<Track>>()
        val processedIds = mutableSetOf<String>()

        for (i in tracks.indices) {
            val trackA = tracks[i]
            if (trackA.id in processedIds) continue

            val currentGroup = mutableListOf<Track>()
            currentGroup.add(trackA)

            for (j in i + 1 until tracks.size) {
                val trackB = tracks[j]
                if (trackB.id in processedIds) continue

                if (isDuplicatePair(trackA, trackB)) {
                    currentGroup.add(trackB)
                    processedIds.add(trackB.id)
                }
            }

            if (currentGroup.size > 1) {
                processedIds.add(trackA.id)
                groups.add(currentGroup)
            }
        }

        // Với mỗi nhóm, sắp xếp tìm ra bài hát chất lượng cao nhất để giữ lại
        return groups.map { groupList ->
            val sortedByQuality = groupList.sortedWith(
                compareByDescending<Track> { getQualityScore(it) }
                    .thenByDescending { getTrackFileSize(context, it) }
            )

            val primary = sortedByQuality.first()
            val duplicates = sortedByQuality.drop(1)
            val reclaimableBytes = duplicates.sumOf { getTrackFileSize(context, it) }

            DuplicateGroup(
                groupKey = primary.id,
                title = primary.title,
                artist = primary.artist,
                primaryTrack = primary,
                duplicateTracks = duplicates,
                totalReclaimableBytes = reclaimableBytes
            )
        }
    }

    /**
     * Kiểm tra 4 tầng an toàn nghiêm ngặt giữa 2 bài hát
     */
    fun isDuplicatePair(trackA: Track, trackB: Track): Boolean {
        // TẦNG 1: Trùng lặp đường dẫn / URL tuyệt đối
        if (trackA.audioUrl.isNotBlank() && trackA.audioUrl == trackB.audioUrl) {
            return true
        }

        // TẦNG 2: So sánh Ca sĩ chuẩn hóa
        val normArtistA = normalizeArtist(trackA.artist)
        val normArtistB = normalizeArtist(trackB.artist)
        if (!areArtistsMatching(normArtistA, normArtistB)) {
            return false // Ca sĩ khác nhau -> Tuyệt đối không trùng
        }

        // TẦNG 3: Kiểm tra Tag Phiên bản (Live, Acoustic, Remix, Instrumental...)
        val versionTagsA = extractVersionTags(trackA.title)
        val versionTagsB = extractVersionTags(trackB.title)
        if (versionTagsA != versionTagsB) {
            // Một bài là Live, một bài là Studio hoặc Remix -> KHÔNG TRÙNG
            return false
        }

        // TẦNG 4: So sánh Tiêu đề sau khi làm sạch
        val cleanTitleA = cleanTitle(trackA.title)
        val cleanTitleB = cleanTitle(trackB.title)

        // Kiểm tra tiêu đề generic (Intro, Outro, Track 01...)
        if (cleanTitleA in GENERIC_TITLES || cleanTitleB in GENERIC_TITLES) {
            val normAlbumA = normalizeString(trackA.album)
            val normAlbumB = normalizeString(trackB.album)
            if (normAlbumA != normAlbumB || normAlbumA.isBlank()) {
                return false // Khác album -> Không coi là trùng
            }
        }

        val titleSimilarity = calculateSimilarity(cleanTitleA, cleanTitleB)
        if (titleSimilarity < 0.88) {
            return false // Tiêu đề không đủ giống nhau -> Bỏ qua
        }

        // TẦNG 5: Kiểm tra Sai số Thời lượng (Duration tolerance <= 3.5 giây)
        if (trackA.durationMs > 0 && trackB.durationMs > 0) {
            val durationDiffMs = abs(trackA.durationMs - trackB.durationMs)
            if (durationDiffMs > 3500L) {
                return false // Thời lượng lệch quá 3.5 giây -> Không thể là cùng 1 bản thu
            }
        }

        return true
    }

    /**
     * Chuẩn hóa văn bản tiếng Việt & ký tự đặc biệt
     */
    fun normalizeString(input: String): String {
        if (input.isBlank()) return ""
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutAccents = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents.lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun cleanTitle(title: String): String {
        var result = title.lowercase()
        // Loại bỏ phần mở rộng file nếu có (.mp3, .flac)
        result = result.substringBeforeLast(".mp3")
            .substringBeforeLast(".flac")
            .substringBeforeLast(".wav")
            .substringBeforeLast(".m4a")

        // Bỏ các từ khóa phụ trong ngoặc: [Official Audio], (Lyrics), (Audio), (Official Music Video)
        result = result.replace("\\[.*?\\]".toRegex(), "")
            .replace("\\(official.*?\\)".toRegex(), "")
            .replace("\\(lyrics.*?\\)".toRegex(), "")
            .replace("\\(audio.*?\\)".toRegex(), "")
            .replace("\\(mv.*?\\)".toRegex(), "")
            .replace("\\(music video.*?\\)".toRegex(), "")
            .replace("- official audio", "")
            .replace("- official video", "")
            .replace("- lyrics", "")

        return normalizeString(result)
    }

    private fun extractVersionTags(title: String): Set<String> {
        val lower = title.lowercase()
        val foundTags = mutableSetOf<String>()
        for (keyword in VERSION_KEYWORDS) {
            if (lower.contains(keyword)) {
                foundTags.add(keyword)
            }
        }
        return foundTags
    }

    private fun normalizeArtist(artist: String): String {
        return normalizeString(artist)
            .replace("feat.", " ")
            .replace("ft.", " ")
            .replace("&", " ")
            .replace("and", " ")
            .replace("x", " ")
    }

    private fun areArtistsMatching(artistA: String, artistB: String): Boolean {
        if (artistA == artistB) return true
        if (artistA.isBlank() || artistB.isBlank()) return false

        val tokensA = artistA.split(" ").filter { it.length > 1 }.toSet()
        val tokensB = artistB.split(" ").filter { it.length > 1 }.toSet()

        if (tokensA.isEmpty() || tokensB.isEmpty()) return false

        // Nếu ca sĩ chính xuất hiện trong cả 2 chuỗi
        val intersection = tokensA.intersect(tokensB)
        val minSize = minOf(tokensA.size, tokensB.size)
        return intersection.size >= minSize || (intersection.size.toDouble() / maxOf(tokensA.size, tokensB.size)) >= 0.6
    }

    /**
     * Tính điểm chất lượng âm thanh để ưu tiên giữ lại bản tốt nhất
     */
    fun getQualityScore(track: Track): Int {
        val ext = track.audioUrl.substringAfterLast('.', "").lowercase()
        val bitRateStr = track.bitRate.lowercase()

        var score = when {
            ext in setOf("dsf", "dff") -> 10000
            track.isHiRes || ext in setOf("flac", "wav", "aiff") -> {
                if (bitRateStr.contains("24-bit") || bitRateStr.contains("96khz") || bitRateStr.contains("192khz")) 9000
                else 7000
            }
            ext in setOf("alac", "m4a") && bitRateStr.contains("lossless") -> 6500
            ext in setOf("m4a", "aac") -> 4000
            ext == "mp3" -> {
                when {
                    bitRateStr.contains("320") -> 3500
                    bitRateStr.contains("256") -> 3000
                    bitRateStr.contains("192") -> 2500
                    bitRateStr.contains("128") -> 1500
                    else -> 2000
                }
            }
            else -> 1000
        }

        // Thưởng thêm điểm nếu có lời bài hát (lyrics)
        if (track.lyrics.isNotEmpty()) {
            score += 200
        }

        // Thưởng điểm nếu có thông số ReplayGain
        if (track.replayGainDb != null) {
            score += 100
        }

        return score
    }

    fun getTrackFileSize(context: Context, track: Track): Long {
        return try {
            val uri = Uri.parse(track.audioUrl)
            if (uri.scheme == "file") {
                val path = uri.path ?: return 0L
                File(path).length()
            } else if (uri.scheme == "content") {
                context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    it.statSize
                } ?: 0L
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Thuật toán Levenshtein Distance tính độ tương đồng 2 chuỗi (0.0 đến 1.0)
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLen = max(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
