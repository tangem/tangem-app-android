package com.tangem.features.collectibles.impl.main.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

/**
 * The list the screen switches to for search.
 *
 * @property listState seeded from the resting list, so the switch itself moves nothing
 * @property firstRowIndex index of the first pack row, i.e. the scroll target that takes the leading
 *   blocks off screen
 */
internal class CollectiblesSearchEntry(val listState: LazyListState, val firstRowIndex: Int)

internal fun LazyListState.leadingBlocksBottom(firstRowIndex: Int): Int = layoutInfo.visibleItemsInfo
    .filter { it.index < firstRowIndex }
    .maxOfOrNull { it.offset + it.size }
    ?.coerceAtLeast(minimumValue = 0)
    ?: 0

internal fun createSearchEntry(mainListState: LazyListState, hasTabs: Boolean) = CollectiblesSearchEntry(
    listState = LazyListState(
        firstVisibleItemIndex = mainListState.firstVisibleItemIndex,
        firstVisibleItemScrollOffset = mainListState.firstVisibleItemScrollOffset,
    ),
    firstRowIndex = if (hasTabs) 2 else 1,
)

/** Stiffness of the scroll into search; a spring's period goes as `1 / sqrt(k)`, so lower is slower. */
private const val LEAD_SCROLL_STIFFNESS = 300f

/** Share of the travel before the top bar over which the leading blocks fade out; zero turns the fade off. */
private const val LEAD_FADE_FRACTION = 0.9f

/**
 * How the leading blocks behave while search is being entered.
 *
 * @property areShown they leave by scrolling off the top, not by collapsing — a collapse shortens the list
 *   under the scroll position, which then gets clamped back. Dropped once the scroll is done.
 * @property alpha fades them out before they reach the blurring top bar, so the drop is never seen.
 */
@Immutable
internal data class CollectiblesSearchLead(val areShown: Boolean, val alpha: Float)

@Composable
internal fun rememberSearchLead(
    mainListState: LazyListState,
    searchEntry: CollectiblesSearchEntry,
    isSearchActive: Boolean,
): CollectiblesSearchLead {
    var areShown by remember(searchEntry) { mutableStateOf(true) }

    val fade = remember(searchEntry) {
        Snapshot.withoutReadObservation {
            // Alpha reaches zero here: the top bar's own height, where the blocks stop being visible.
            val hiddenAt = mainListState.layoutInfo.beforeContentPadding
            val visibleTravel =
                mainListState.leadingBlocksBottom(firstRowIndex = searchEntry.firstRowIndex) - hiddenAt

            hiddenAt to visibleTravel.coerceAtLeast(minimumValue = 0) * LEAD_FADE_FRACTION
        }
    }
    val alpha by remember(searchEntry, fade) {
        derivedStateOf {
            val (hiddenAt, fadeSpan) = fade
            if (fadeSpan <= 0f) return@derivedStateOf 1f

            val bottom = searchEntry.listState.leadingBlocksBottom(firstRowIndex = searchEntry.firstRowIndex)
            ((bottom - hiddenAt) / fadeSpan).coerceIn(minimumValue = 0f, maximumValue = 1f)
        }
    }

    LaunchedEffect(searchEntry, isSearchActive) {
        if (!isSearchActive) return@LaunchedEffect

        // Scrolling by what the blocks still reach into the viewport puts the first row at its top; already
        // past them, that distance is zero and nothing moves.
        val distance = searchEntry.listState.leadingBlocksBottom(firstRowIndex = searchEntry.firstRowIndex)
        if (distance > 0) {
            searchEntry.listState.animateScrollBy(
                value = distance.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = LEAD_SCROLL_STIFFNESS,
                    visibilityThreshold = 0.5f,
                ),
            )
        }
        areShown = false
    }

    return CollectiblesSearchLead(
        areShown = !isSearchActive || areShown,
        alpha = if (isSearchActive) alpha else 1f,
    )
}