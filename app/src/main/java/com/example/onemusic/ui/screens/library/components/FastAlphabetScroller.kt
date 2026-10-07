package com.example.onemusic.ui.screens.library.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.components.ApexAlphabetScroller
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import kotlinx.coroutines.launch

/**
 * Fast Alphabet Scroller on the right edge with letter index lookup and scrolling.
 */
@Composable
fun FastAlphabetScroller(
    availableLetters: Set<Char>,
    sortedTracks: List<Track>,
    listState: LazyListState,
    headerOffsetCount: Int = 4,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    ApexAlphabetScroller(
        onLetterSelected = { char: Char ->
            val targetIndex = sortedTracks.indexOfFirst { track ->
                val firstChar = track.title.trim().firstOrNull()?.uppercaseChar() ?: '#'
                if (char == '#') !firstChar.isLetter() else firstChar == char
            }

            if (targetIndex != -1) {
                scope.launch {
                    listState.scrollToItem(targetIndex + headerOffsetCount)
                }
            }
        },
        availableLetters = availableLetters,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 246.dp, bottom = LocalBottomOverlayPadding.current, end = 2.dp)
    )
}
