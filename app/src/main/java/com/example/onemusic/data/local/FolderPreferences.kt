package com.example.onemusic.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.example.onemusic.data.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Immutable
@Serializable
data class FolderInfo(
    val uriString: String,
    val displayName: String,
    val trackCount: Int = 0
)


class FolderPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("onemusic_folder_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getFolders(): List<FolderInfo> {
        val raw = prefs.getString("saved_folders", null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveFolders(folders: List<FolderInfo>) {
        try {
            val raw = json.encodeToString(folders)
            prefs.edit().putString("saved_folders", raw).apply()
        } catch (_: Throwable) {}
    }

    fun addFolder(uriString: String, displayName: String) {
        val current = getFolders().toMutableList()
        if (current.none { it.uriString == uriString }) {
            current.add(FolderInfo(uriString = uriString, displayName = displayName, trackCount = 0))
            saveFolders(current)
        }
    }

    fun removeFolder(uriString: String) {
        val updated = getFolders().filter { it.uriString != uriString }
        saveFolders(updated)
    }

    fun updateFolderTrackCount(uriString: String, count: Int) {
        val updated = getFolders().map {
            if (it.uriString == uriString) it.copy(trackCount = count) else it
        }
        saveFolders(updated)
    }

    fun getCachedTracks(): List<Track> {
        val raw = prefs.getString("cached_local_tracks", null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCachedTracks(tracks: List<Track>) {
        try {
            val raw = json.encodeToString(tracks)
            prefs.edit().putString("cached_local_tracks", raw).apply()
        } catch (_: Exception) {}
    }

    fun getHiddenTrackIds(): Set<String> {
        return prefs.getStringSet("hidden_duplicate_track_ids", emptySet()) ?: emptySet()
    }

    fun hideTracks(trackIds: Set<String>) {
        val current = getHiddenTrackIds().toMutableSet()
        current.addAll(trackIds)
        prefs.edit().putStringSet("hidden_duplicate_track_ids", current).apply()
    }

    fun unhideAllTracks() {
        prefs.edit().remove("hidden_duplicate_track_ids").apply()
    }
}
