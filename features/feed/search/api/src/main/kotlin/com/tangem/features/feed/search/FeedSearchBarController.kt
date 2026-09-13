package com.tangem.features.feed.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared state of the feed's search bar. The search bar itself is host chrome (the shtorka sheet
 * header), while search results are a feed screen in another module — this controller is the seam
 * between them:
 *
 * - the host's search bar reports typing and focus via [onQueryChange]/[onActiveChange];
 * - the feed host reacts to [FeedSearchBarUM.isActive] by pushing/popping the search route;
 * - the search screen observes [FeedSearchBarUM.query] to drive results.
 */
@Stable
interface FeedSearchBarController {

    val state: StateFlow<FeedSearchBarUM>

    /**
     * Emits when the user commits the query from the keyboard's search action. Carries nothing —
     * the query itself is in [state] — and is not replayed, so a screen that starts collecting later
     * does not see earlier submissions.
     */
    val submits: SharedFlow<Unit>

    fun onQueryChange(query: String)

    /** Focus/unfocus of the search bar; `true` opens the search screen, `false` closes it. */
    fun onActiveChange(isActive: Boolean)

    fun onSubmit()
}

@Immutable
data class FeedSearchBarUM(
    val query: String = "",
    val isActive: Boolean = false,
)