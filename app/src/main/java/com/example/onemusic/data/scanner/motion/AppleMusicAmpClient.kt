package com.example.onemusic.data.scanner.motion

import android.content.SharedPreferences
import android.util.Log
import com.example.onemusic.data.scanner.motion.MotionHttpClient.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class EditorialVideoResult(
    val preferredVideoUrl: String,
    val tallUrl: String = "",
    val squareUrl: String = "",
    val isTall: Boolean = false
)

/**
 * Bước 2: hỏi Apple Music AMP API lấy `editorialVideo` (video bìa động) của một album.
 * Giữ JWT hiện tại (lưu trong [prefs]); bị từ chối thì lấy JWT mới từ trang web Apple Music rồi thử lại 1 lần.
 */
internal class AppleMusicAmpClient(private val prefs: SharedPreferences) {

    companion object {
        private const val AMP_API_URL = "https://amp-api.music.apple.com/v1/catalog/us/albums"
        private const val APPLE_MUSIC_WEB_URL = "https://music.apple.com"

        private const val DEFAULT_JWT_TOKEN =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzg2NjMyOTI0LCJleHAiOjE3OTI2ODA5MjQsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.hBgj61sZf-y7bmuvT-joXAUAcf7TVJ51732xnH5vFkLHOmsQHxVqGMYUuI4h8c0-RX3fRY3moylhLW8fewFJyw"

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** Đọc URL video vuông (ưu tiên) và dọc từ JSON của AMP API. Hàm thuần, không gọi mạng. */
        fun parseEditorialVideo(responseJson: String): EditorialVideoResult? {
            return runCatching {
                val root = json.parseToJsonElement(responseJson).jsonObject
                val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@runCatching null
                val attributes = data["attributes"]?.jsonObject ?: return@runCatching null
                val editorialVideo = attributes["editorialVideo"]?.jsonObject ?: return@runCatching null

                // 1. Square formats FIRST (motionDetailSquare / motionSquareVideo1x1 / motionArtistSquare1x1 for 1:1 square matching album artwork)
                val squareKeys = listOf("motionDetailSquare", "motionSquareVideo1x1", "motionArtistSquare1x1")
                var squareUrl: String? = null
                for (key in squareKeys) {
                    val v = editorialVideo[key]?.jsonObject?.get("video")?.jsonPrimitive?.content
                    if (!v.isNullOrBlank() && v.contains("mvod.itunes.apple.com")) {
                        squareUrl = v
                        break
                    }
                }

                // 2. Tall formats fallback (motionDetailTall / motionTallVideo3x4)
                val tallKeys = listOf("motionDetailTall", "motionTallVideo3x4")
                var tallUrl: String? = null
                for (key in tallKeys) {
                    val v = editorialVideo[key]?.jsonObject?.get("video")?.jsonPrimitive?.content
                    if (!v.isNullOrBlank() && v.contains("mvod.itunes.apple.com")) {
                        tallUrl = v
                        break
                    }
                }

                val preferred = squareUrl ?: tallUrl ?: return@runCatching null

                EditorialVideoResult(
                    preferredVideoUrl = preferred,
                    tallUrl = tallUrl ?: "",
                    squareUrl = squareUrl ?: "",
                    isTall = tallUrl != null && squareUrl == null
                )
            }.getOrNull()
        }
    }

    private var currentJwtToken: String = prefs.getString("jwt_token", DEFAULT_JWT_TOKEN) ?: DEFAULT_JWT_TOKEN

    /**
     * Query Apple Music AMP API directly to extract editorialVideo motion artwork URL.
     * Prioritizes square formats, falls back to tall formats.
     */
    suspend fun fetchEditorialVideo(collectionId: String): EditorialVideoResult? =
        withContext(Dispatchers.IO) {
            val apiUrl = "$AMP_API_URL/$collectionId?extend=editorialVideo,editorialArtwork"

            var responseJson = MotionHttpClient.getWithAuth(apiUrl, currentJwtToken)

            // If 401 Unauthorized, refresh the token and retry once
            if (responseJson == null) {
                Log.w(TAG, "AMP API request failed, attempting to refresh JWT token...")
                val refreshedToken = refreshJwtTokenFromWeb()
                if (refreshedToken != null && refreshedToken != currentJwtToken) {
                    currentJwtToken = refreshedToken
                    responseJson = MotionHttpClient.getWithAuth(apiUrl, currentJwtToken)
                }
            }

            if (responseJson == null) return@withContext null

            return@withContext parseEditorialVideo(responseJson)
        }

    private suspend fun refreshJwtTokenFromWeb(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val browseHtml = MotionHttpClient.get("$APPLE_MUSIC_WEB_URL/us/browse") ?: return@runCatching null
            val jsPathMatch = Regex("""src="(/assets/index~[a-zA-Z0-9_-]+\.js)"""").find(browseHtml)
            val jsPath = jsPathMatch?.groupValues?.get(1) ?: return@runCatching null

            val jsContent = MotionHttpClient.get("$APPLE_MUSIC_WEB_URL$jsPath") ?: return@runCatching null
            val tokenMatch = Regex("""(eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,})""").find(jsContent)
            val token = tokenMatch?.groupValues?.get(1)
            if (!token.isNullOrBlank()) {
                Log.d(TAG, "Successfully extracted fresh JWT token from Apple Music web")
                prefs.edit().putString("jwt_token", token).apply()
                token
            } else null
        }.getOrNull()
    }
}
