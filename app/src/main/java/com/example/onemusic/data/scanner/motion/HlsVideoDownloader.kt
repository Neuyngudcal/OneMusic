package com.example.onemusic.data.scanner.motion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Bước 3 & 4: đọc playlist HLS (M3U8) của video bìa động, chọn bản AVC/H.264 tốt nhất ≤ 1080p,
 * rồi tải về một tệp MP4 (trực tiếp nếu có EXT-X-MAP trỏ tới .mp4, hoặc ghép các đoạn HLS).
 */
internal object HlsVideoDownloader {

    /**
     * Inspects a variant m3u8 playlist to see if it uses EXT-X-MAP pointing to a single .mp4 file.
     */
    suspend fun extractDirectMp4UrlFromVariant(variantM3u8Url: String): String? =
        withContext(Dispatchers.IO) {
            val content = MotionHttpClient.get(variantM3u8Url) ?: return@withContext null
            findDirectMp4Url(content, variantM3u8Url)
        }

    /** Tìm tệp .mp4 trong `#EXT-X-MAP:URI="…"` của playlist biến thể. Hàm thuần. */
    fun findDirectMp4Url(content: String, variantM3u8Url: String): String? {
        val mapMatch = Regex("""#EXT-X-MAP:URI="([^"]+\.mp4)"""").find(content)
        val mp4RelativeOrAbsolute = mapMatch?.groupValues?.get(1) ?: return null
        return MotionHttpClient.resolveUrl(variantM3u8Url, mp4RelativeOrAbsolute)
    }

    /**
     * Parse HLS master playlist and select best AVC/H.264 variant (up to 1080p).
     */
    suspend fun parseMasterPlaylistForBestVariant(m3u8Url: String): String? =
        withContext(Dispatchers.IO) {
            val content = MotionHttpClient.get(m3u8Url) ?: return@withContext null
            selectBestVariant(content, m3u8Url)
        }

    /** Chọn biến thể tốt nhất từ nội dung master playlist: ưu tiên AVC/H.264, độ phân giải cao nhất ≤ 1080p. Hàm thuần. */
    fun selectBestVariant(content: String, m3u8Url: String): String? {
        val lines = content.lines()

        data class Variant(val bandwidth: Long, val resolution: Int, val codec: String, val url: String)

        val variants = mutableListOf<Variant>()
        var currentBandwidth = 0L
        var currentResolution = 0
        var currentCodec = ""

        for (line in lines) {
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val bwMatch = Regex("""BANDWIDTH=(\d+)""").find(line)
                currentBandwidth = bwMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

                val resMatch = Regex("""RESOLUTION=(\d+)x(\d+)""").find(line)
                currentResolution = resMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

                val codecMatch = Regex("""CODECS="([^"]+)"""").find(line)
                currentCodec = codecMatch?.groupValues?.get(1) ?: ""
            } else if (!line.startsWith("#") && line.isNotBlank() && currentBandwidth > 0) {
                val absoluteUrl = MotionHttpClient.resolveUrl(m3u8Url, line.trim())
                variants.add(Variant(currentBandwidth, currentResolution, currentCodec, absoluteUrl))
                currentBandwidth = 0L
                currentResolution = 0
                currentCodec = ""
            }
        }

        if (variants.isEmpty()) return null

        // Prioritize highest quality 1080p AVC/H.264 variant for crystal-sharp display
        val avcVariants = variants.filter { it.codec.contains("avc1", ignoreCase = true) }
        val pool = if (avcVariants.isNotEmpty()) avcVariants else variants

        // Pick 1080p (or highest available resolution <= 1080p)
        val highestQualityVariant = pool.filter { it.resolution <= 1080 }.maxByOrNull { it.resolution }
            ?: pool.maxByOrNull { it.resolution }
            ?: pool.firstOrNull()

        return highestQualityVariant?.url
    }

    suspend fun downloadFile(url: String, outputFile: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val urlObj = URL(url)
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = MotionHttpClient.CONNECT_TIMEOUT_MS
                conn.readTimeout = MotionHttpClient.READ_TIMEOUT_MS * 2
                conn.setRequestProperty("User-Agent", MotionHttpClient.USER_AGENT)

                try {
                    if (conn.responseCode != 200 && conn.responseCode != 206) return@runCatching false
                    val buffer = ByteArray(128 * 1024)
                    java.io.BufferedInputStream(conn.inputStream, 128 * 1024).use { bis ->
                        java.io.BufferedOutputStream(FileOutputStream(outputFile), 128 * 1024).use { bos ->
                            var bytesRead: Int
                            while (bis.read(buffer).also { bytesRead = it } != -1) {
                                bos.write(buffer, 0, bytesRead)
                            }
                            bos.flush()
                        }
                    }
                    outputFile.length() > 0
                } finally {
                    conn.disconnect()
                }
            }.getOrElse { false }
        }

    suspend fun downloadHlsVariant(variantM3u8Url: String, outputFile: File): Boolean =
        withContext(Dispatchers.IO) {
            val content = MotionHttpClient.get(variantM3u8Url) ?: return@withContext false
            val lines = content.lines()

            // 1. Check for fMP4 initialization segment (#EXT-X-MAP:URI="init.mp4")
            val initMapLine = lines.firstOrNull { it.startsWith("#EXT-X-MAP:") }
            val initUri = initMapLine?.let { Regex("""URI="([^"]+)"""").find(it)?.groupValues?.get(1) }
            val initUrl = initUri?.let { MotionHttpClient.resolveUrl(variantM3u8Url, it.trim()) }

            val segmentUrls = lines.filter { !it.startsWith("#") && it.isNotBlank() }
                .map { MotionHttpClient.resolveUrl(variantM3u8Url, it.trim()) }

            if (segmentUrls.isEmpty()) return@withContext false

            runCatching {
                coroutineScope {
                    // Download init segment and all video chunks concurrently in parallel
                    val initDeferred = initUrl?.let { url ->
                        async(Dispatchers.IO) { MotionHttpClient.getBytes(url) }
                    }

                    val segmentsDeferred = segmentUrls.mapIndexed { index, segUrl ->
                        async(Dispatchers.IO) {
                            Pair(index, MotionHttpClient.getBytes(segUrl))
                        }
                    }

                    val initBytes = initDeferred?.await()
                    val downloadedSegments = segmentsDeferred.awaitAll()

                    FileOutputStream(outputFile).use { fos ->
                        // Write fMP4 header (ftyp + moov) first so ExoPlayer can decode the file from disk
                        if (initBytes != null && initBytes.isNotEmpty()) {
                            fos.write(initBytes)
                        }
                        for ((_, segData) in downloadedSegments.sortedBy { it.first }) {
                            if (segData != null && segData.isNotEmpty()) {
                                fos.write(segData)
                            }
                        }
                    }
                }
                outputFile.length() > 0
            }.getOrElse { false }
        }
}
