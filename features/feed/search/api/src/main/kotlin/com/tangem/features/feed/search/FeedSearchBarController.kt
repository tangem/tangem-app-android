package com.tangem.features.feed.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared state of the feed's search bar. The search bar itself is host chrome (the shtorka sheet
 * header), while search results are a feed screen in another module — this controller is the seam
 * between them:
 *
 * - the host's search bar reports typing, focus and its close button via [onQueryChange]/[onFocusChange]/[onClose];
 * - the feed host opens the search route on the first typed character and closes it on [onClose];
 * - the search screen observes [FeedSearchBarUM.query] to drive results.
 */
@Stable
interface FeedSearchBarController {

    val state: StateFlow<FeedSearchBarUM>

    fun onQueryChange(query: String)

    /** Focus/unfocus of the search bar. Focus alone keeps the current feed screen; typing opens the search. */
    fun onFocusChange(isFocused: Boolean)

    /** The search bar's close button: leaves the search screen and drops the query. */
    fun onClose()
}

/**
 * @property isActive the bar is in a search session: focused, or the search screen is open
 */
@Immutable
data class FeedSearchBarUM(
    val query: String = "",
    val isActive: Boolean = false,
)