package com.example.onemusic.data.scanner

import com.example.onemusic.data.model.LyricLine
import com.example.onemusic.data.model.LyricWord

/**
 * Standard & Enhanced LRC (Lyrics) format parser supporting:
 * - Standard timestamps: `[mm:ss.xx]`, `[mm:ss.xxx]`, `[mm:ss]`
 * - Word-by-Word Enhanced LRC tags: `<mm:ss.xx> word <mm:ss.xx> word`
 * - Word duration tags: `word(durationMs)` or `word(startMs,durationMs)`
 * - Smart Word-Interpolation: Generates natural syllable/word timings for standard LRC lines
 * - Header offset adjustments: `[offset:+500]`
 * - LRCLIB synced & plain lyrics formats
 */
object LrcParser {

    private val LINE_TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")
    private val WORD_TIMESTAMP_REGEX = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>""")
    private val WORD_DURATION_REGEX = Regex("""([^\s\(\)]+)\((\d+)(?:,\s*(\d+))?\)""")
    private val OFFSET_REGEX = Regex("""\[offset:\s*([+-]?\d+)\s*\]""", RegexOption.IGNORE_CASE)

    /**
     * Parses LRC string content into a sorted list of [LyricLine] with word-by-word timings.
     */
    fun parseLrc(content: String, durationMs: Long = 0L): List<LyricLine> {
        if (content.isBlank()) return emptyList()

        return try {
            val lines = content.lines()
            var offsetMs = 0L

            // Check for [offset: +/-ms]
            for (line in lines) {
                val offsetMatch = OFFSET_REGEX.find(line)
                if (offsetMatch != null) {
                    offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                    break
                }
            }

            val rawParsedLines = mutableListOf<LyricLine>()

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isBlank()) continue

                val matches = LINE_TIMESTAMP_REGEX.findAll(line).toList()
                if (matches.isNotEmpty()) {
                    val lineBody = LINE_TIMESTAMP_REGEX.replace(line, "").trim()
                    if (lineBody.isNotBlank()) {
                        for (match in matches) {
                            val lineStartMs = parseTimestamp(
                                match.groupValues[1],
                                match.groupValues[2],
                                match.groupValues[3]
                            ) + offsetMs

                            // 1. Try parsing Enhanced LRC word timestamps (<mm:ss.xx>)
                            val explicitWords = parseEnhancedWordTags(lineBody, lineStartMs, offsetMs)
                            val isExplicitEnhanced = explicitWords.isNotEmpty()

                            // Clean visible text (strip word tags)
                            val cleanText = cleanVisibleText(lineBody)
                            if (cleanText.isNotBlank() && !cleanText.equals("null", ignoreCase = true) && !cleanText.equals("(null)", ignoreCase = true)) {
                                rawParsedLines.add(
                                    LyricLine(
                                        timestampMs = maxOf(0L, lineStartMs),
                                        text = cleanText,
                                        words = explicitWords,
                                        isEnhanced = isExplicitEnhanced
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val sortedLines = rawParsedLines.sortedBy { it.timestampMs }
            if (sortedLines.isNotEmpty()) {
                return generateMissingWordTimings(sortedLines, durationMs)
            }

            // Fallback for plain unsynchronized text (e.g. from plainLyrics)
            val nonTagLines = lines.map { cleanVisibleText(it) }
                .filter { it.isNotBlank() && !it.startsWith('[') && !it.startsWith('#') && !it.equals("null", ignoreCase = true) && !it.equals("(null)", ignoreCase = true) }

            if (nonTagLines.isNotEmpty()) {
                val count = nonTagLines.size
                val effectiveDuration = if (durationMs > 0) durationMs else (count * 4000L)
                val interval = effectiveDuration / count

                val plainLines = nonTagLines.mapIndexed { index, text ->
                    LyricLine(timestampMs = index * interval, text = text, isSynced = false)
                }
                return generateMissingWordTimings(plainLines, durationMs)
            }

            emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseTimestamp(minStr: String, secStr: String, fracStr: String): Long {
        val minutes = minStr.toLongOrNull() ?: 0L
        val seconds = secStr.toLongOrNull() ?: 0L
        val millis = when (fracStr.length) {
            1 -> (fracStr.toLongOrNull() ?: 0L) * 100
            2 -> (fracStr.toLongOrNull() ?: 0L) * 10
            3 -> fracStr.toLongOrNull() ?: 0L
            else -> 0L
        }
        return (minutes * 60 * 1000) + (seconds * 1000) + millis
    }

    /**
     * Parses word tags like `<00:12.34> word <00:12.80> word` or `word(300) word(500)`.
     */
    private fun parseEnhancedWordTags(lineBody: String, lineStartMs: Long, offsetMs: Long): List<LyricWord> {
        val words = mutableListOf<LyricWord>()

        // Check for <mm:ss.xx> word format
        if (WORD_TIMESTAMP_REGEX.containsMatchIn(lineBody)) {
            val tokens = mutableListOf<Pair<Long, String>>()
            val matches = WORD_TIMESTAMP_REGEX.findAll(lineBody).toList()

            for (i in matches.indices) {
                val match = matches[i]
                val wordTime = parseTimestamp(
                    match.groupValues[1],
                    match.groupValues[2],
                    match.groupValues[3]
                ) + offsetMs

                val textStart = match.range.last + 1
                val textEnd = if (i < matches.lastIndex) matches[i + 1].range.first else lineBody.length
                val rawWordText = lineBody.substring(textStart, textEnd).trim()

                // Filter out empty text or residual tags from pair tags (<start>word<end>)
                if (rawWordText.isNotBlank() && !WORD_TIMESTAMP_REGEX.matches(rawWordText)) {
                    tokens.add(wordTime to rawWordText)
                }
            }

            for (i in tokens.indices) {
                val (startMs, text) = tokens[i]
                val endMs = if (i < tokens.lastIndex) tokens[i + 1].first else startMs + 1200L
                words.add(LyricWord(text = text, startMs = maxOf(0L, startMs), endMs = maxOf(startMs + 120L, endMs)))
            }
            return words
        }

        // Check for word(durationMs) or word(startOffset,durationMs) format
        if (WORD_DURATION_REGEX.containsMatchIn(lineBody)) {
            var currentStart = lineStartMs
            for (match in WORD_DURATION_REGEX.findAll(lineBody)) {
                val text = match.groupValues[1].trim()
                val firstVal = match.groupValues[2].toLongOrNull() ?: 300L
                val secondVal = match.groupValues[3].toLongOrNull()

                if (text.isNotBlank()) {
                    if (secondVal != null) {
                        // Format: word(offsetMs, durationMs)
                        val wordStart = lineStartMs + firstVal
                        val wordEnd = wordStart + secondVal
                        words.add(LyricWord(text = text, startMs = maxOf(0L, wordStart), endMs = maxOf(wordStart + 100L, wordEnd)))
                    } else {
                        // Format: word(durationMs)
                        val wordEnd = currentStart + firstVal
                        words.add(LyricWord(text = text, startMs = maxOf(0L, currentStart), endMs = maxOf(currentStart + 100L, wordEnd)))
                        currentStart = wordEnd + 40L
                    }
                }
            }
            return words
        }

        return emptyList()
    }

    private fun cleanVisibleText(lineBody: String): String {
        return lineBody.replace(WORD_TIMESTAMP_REGEX, "")
            .replace(Regex("""\(\d+(?:,\s*\d+)?\)"""), "")
            .trim()
            .replace(Regex("""\s+"""), " ")
    }

    /**
     * Interpolates smooth syllable-weighted word timings for lines that do not have explicit word tags.
     */
    private fun generateMissingWordTimings(lines: List<LyricLine>, totalDurationMs: Long): List<LyricLine> {
        return lines.mapIndexed { index, line ->
            if (line.words.isNotEmpty()) {
                line.copy(isEnhanced = true)
            } else {
                val nextTimestamp = if (index < lines.lastIndex) lines[index + 1].timestampMs else {
                    if (totalDurationMs > line.timestampMs) totalDurationMs else line.timestampMs + 5000L
                }

                val lineRawDuration = (nextTimestamp - line.timestampMs).coerceIn(1500L, 8000L)
                // Active singing duration leaves a brief 250ms breath window at the end
                val activeSingingDuration = (lineRawDuration - 250L).coerceAtLeast(1000L)

                val rawWords = line.text.split(Regex("""\s+""")).filter { it.isNotBlank() }
                if (rawWords.isEmpty()) {
                    line
                } else {
                    // Weight duration per word by character count + 2 (representing base syllable duration)
                    val weights = rawWords.map { maxOf(2, it.length) }
                    val totalWeight = weights.sum().toFloat()

                    var wordStart = line.timestampMs
                    val generatedWords = mutableListOf<LyricWord>()

                    for (wIdx in rawWords.indices) {
                        val wordText = rawWords[wIdx]
                        val fraction = weights[wIdx] / totalWeight
                        val wordDuration = (activeSingingDuration * fraction).toLong().coerceAtLeast(150L)
                        val wordEnd = wordStart + wordDuration

                        generatedWords.add(LyricWord(text = wordText, startMs = wordStart, endMs = wordEnd))
                        wordStart = wordEnd
                    }

                    // Enable isEnhanced for standard synced lines with generated words!
                    line.copy(words = generatedWords, isEnhanced = line.isSynced)
                }
            }
        }
    }
}
