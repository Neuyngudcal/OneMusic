package com.example.onemusic.data.search

import com.example.onemusic.data.model.Track

object ArtistExtractor {

    private val FEAT_PAREN_REGEX = Regex("(?i)\\s*\\((?:feat\\.|ft\\.|featuring)\\s+[^)]+\\)")
    private val FEAT_BRACKET_REGEX = Regex("(?i)\\[(?:feat\\.|ft\\.|featuring)\\s+[^]]+\\]")

    // Strict multi-artist delimiter: symbols like comma/semicolon/slash OR whole words with surrounding whitespace
    private val ARTIST_SPLIT_REGEX = Regex(
        """\s*[,;/]\s*|\s+(?:&|\+|feat\.?|ft\.?|featuring|with|vs\.?|[xX])\s+""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Splits compound artist strings like "Aura Bloom & Samsung SoundLab"
     * into separate individual artist names: ["Aura Bloom", "Samsung SoundLab"].
     * Safely preserves single artists with 'x' in their names (e.g. "Charli xcx", "Lil Nas X", "The xx").
     */
    fun extractIndividualArtists(rawArtist: String?): List<String> {
        if (rawArtist.isNullOrBlank()) return emptyList()

        var cleaned = rawArtist.trim()
        cleaned = FEAT_PAREN_REGEX.replace(cleaned, "")
        cleaned = FEAT_BRACKET_REGEX.replace(cleaned, "")

        // Remove specific trailing parenthesized metadata tags like (Remix), (Live), (Official), etc.
        if (cleaned.endsWith(")") && cleaned.contains("(")) {
            val insideParen = cleaned.substringAfterLast("(").substringBefore(")")
            if (insideParen.length <= 6 ||
                insideParen.contains("MO", ignoreCase = true) ||
                insideParen.contains("Live", ignoreCase = true) ||
                insideParen.contains("Remix", ignoreCase = true) ||
                insideParen.contains("Audio", ignoreCase = true) ||
                insideParen.contains("Official", ignoreCase = true)
            ) {
                cleaned = cleaned.substringBeforeLast("(").trim()
            }
        }

        val parts = cleaned
            .split(ARTIST_SPLIT_REGEX)
            .map { it.trim().trim(';', '/', ',', '&', '-', '(', ')', '[', ']', '"', '\'') }
            .filter {
                it.isNotBlank() &&
                it.length > 1 &&
                !it.equals("Unknown", ignoreCase = true) &&
                !it.contains("chưa rõ", ignoreCase = true)
            }
            .distinct()

        return if (parts.isEmpty()) {
            val fallback = rawArtist.trim()
            if (fallback.isNotBlank() && !fallback.equals("Unknown", ignoreCase = true) && !fallback.contains("chưa rõ", ignoreCase = true)) {
                listOf(fallback)
            } else {
                emptyList()
            }
        } else {
            parts
        }
    }

    /**
     * Resolves and merges prefix aliases into full canonical artist names.
     * E.g. If both "Charli" and "Charli xcx" exist, merges all tracks of "Charli" into "Charli xcx".
     */
    fun mergeCanonicalArtists(rawMap: Map<String, List<Track>>): Map<String, List<Track>> {
        if (rawMap.isEmpty()) return emptyMap()

        val canonicalMap = mutableMapOf<String, MutableList<Track>>()
        // Sort keys by length DESC so full names (e.g. "Charli xcx") are evaluated first
        val sortedKeys = rawMap.keys.sortedByDescending { it.length }
        val aliasMap = mutableMapOf<String, String>()

        for (key in sortedKeys) {
            // Find if this key is a prefix alias of an already registered longer artist name
            val targetCanonical = aliasMap[key] ?: sortedKeys.firstOrNull { longerKey ->
                longerKey.length > key.length &&
                (longerKey.startsWith("$key ", ignoreCase = true) ||
                 longerKey.startsWith("${key}x", ignoreCase = true) ||
                 longerKey.startsWith("${key}_", ignoreCase = true))
            }

            if (targetCanonical != null && targetCanonical != key) {
                aliasMap[key] = targetCanonical
                canonicalMap.getOrPut(targetCanonical) { mutableListOf() }.addAll(rawMap[key].orEmpty())
            } else {
                canonicalMap.getOrPut(key) { mutableListOf() }.addAll(rawMap[key].orEmpty())
            }
        }

        return canonicalMap.mapValues { (_, tracks) -> tracks.distinctBy { it.id } }
    }
}
