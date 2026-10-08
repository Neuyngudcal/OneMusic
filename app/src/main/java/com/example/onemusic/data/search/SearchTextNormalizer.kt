package com.example.onemusic.data.search

import java.text.Normalizer
import java.util.Locale

object SearchTextNormalizer {

    private val COMBINING_DIACRITICS_REGEX = Regex("""\p{M}+""")
    private val PUNCTUATION_AND_SPECIALS = Regex("""[^\p{L}\p{Nd}\s]""")
    private val MULTIPLE_SPACES = Regex("""\s+""")

    /**
     * Normalizes text by:
     * 1. Converting to lowercase
     * 2. Converting Vietnamese specific letters (đ/Đ -> d)
     * 3. Stripping all accents and diacritical marks (NFD)
     * 4. Removing special characters/punctuations (&, -, +, etc.)
     * 5. Collapsing consecutive whitespaces
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""

        // Step 1 & 2: Lowercase and replace Vietnamese specific 'đ' / 'Đ'
        val lower = input.lowercase(Locale.ROOT)
            .replace('đ', 'd')
            .replace('Đ', 'd')

        // Step 3: NFD decomposition to remove accents
        val nfd = Normalizer.normalize(lower, Normalizer.Form.NFD)
        val withoutAccents = COMBINING_DIACRITICS_REGEX.replace(nfd, "")

        // Step 4: Remove punctuation, separators (&, +, -, /, etc.)
        val cleanChars = PUNCTUATION_AND_SPECIALS.replace(withoutAccents, " ")

        // Step 5: Trim and collapse whitespace
        return MULTIPLE_SPACES.replace(cleanChars, " ").trim()
    }

    /**
     * Splits normalized text into individual word tokens.
     */
    fun tokenize(input: String?): List<String> {
        val normalized = normalize(input)
        if (normalized.isBlank()) return emptyList()
        return normalized.split(' ').filter { it.isNotBlank() }
    }
}
