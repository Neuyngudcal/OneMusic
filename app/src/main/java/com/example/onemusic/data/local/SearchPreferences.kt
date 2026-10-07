package com.example.onemusic.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RecentSearchEntry(
    val query: String,
    val subtitle: String = "Lịch sử tìm kiếm",
    val timestamp: Long = System.currentTimeMillis()
)

class SearchPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("onemusic_search_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val MAX_RECENT_SEARCHES = 15
    }

    fun getRecentSearches(): List<RecentSearchEntry> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<RecentSearchEntry>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentSearch(query: String, subtitle: String = "Lịch sử tìm kiếm") {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        val current = getRecentSearches().filterNot { it.query.equals(trimmed, ignoreCase = true) }.toMutableList()
        current.add(0, RecentSearchEntry(query = trimmed, subtitle = subtitle, timestamp = System.currentTimeMillis()))

        val trimmedList = current.take(MAX_RECENT_SEARCHES)
        saveRecentSearches(trimmedList)
    }

    fun removeRecentSearch(query: String) {
        val updated = getRecentSearches().filterNot { it.query.equals(query, ignoreCase = true) }
        saveRecentSearches(updated)
    }

    fun clearAllRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
    }

    fun clearRecentSearches() {
        clearAllRecentSearches()
    }

    private fun saveRecentSearches(list: List<RecentSearchEntry>) {
        try {
            val raw = json.encodeToString(list)
            prefs.edit().putString(KEY_RECENT_SEARCHES, raw).apply()
        } catch (_: Exception) {}
    }
}
