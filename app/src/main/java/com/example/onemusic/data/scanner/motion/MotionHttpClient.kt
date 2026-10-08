package com.example.onemusic.data.scanner.motion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** HTTP GET dùng chung cho iTunes, Apple Music AMP và tải video HLS (User-Agent trình duyệt, timeout). */
internal object MotionHttpClient {
    const val TAG = "MotionFetcher"
    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 25_000

    val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    suspend fun getWithAuth(urlString: String, bearerToken: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Authorization", "Bearer $bearerToken")
                conn.setRequestProperty("Origin", "https://music.apple.com")
                conn.setRequestProperty("Accept", "application/json")

                try {
                    if (conn.responseCode != 200) {
                        Log.w(TAG, "HTTP ${conn.responseCode} for $urlString")
                        return@runCatching null
                    }
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }

    suspend fun get(urlString: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")

            try {
                if (conn.responseCode != 200) {
                    Log.w(TAG, "HTTP ${conn.responseCode} for $urlString")
                    return@runCatching null
                }
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    suspend fun getBytes(urlString: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS * 2
            conn.setRequestProperty("User-Agent", USER_AGENT)

            try {
                if (conn.responseCode != 200) return@runCatching null
                conn.inputStream.use { it.readBytes() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** Ghép đường dẫn tương đối trong playlist M3U8 với URL của chính playlist đó. */
    fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl
        }
        val lastSlash = baseUrl.lastIndexOf('/')
        return if (lastSlash > 0) {
            baseUrl.substring(0, lastSlash + 1) + relativeUrl
        } else {
            relativeUrl
        }
    }
}
