package com.tangem.features.feed.components.v2

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.tangem.features.feed.search.FeedSearchBarController
import com.tangem.features.feed.search.FeedSearchBarUM
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultFeedSearchBarController @Inject constructor() : FeedSearchBarController {

    override val state: StateFlow<FeedSearchBarUM>
        field = MutableStateFlow(FeedSearchBarUM())

    /**
     * Whether the search screen should be on the feed stack; the feed host brings the stack in line
     * with it. A state rather than events: typing and clearing faster than the host navigates collapses
     * to the latest wish instead of leaving the screen open under a blank bar.
     */
    val isSearchScreenRequested: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private var isFocused = false
    private var isSearchScreenShown = false

    override fun onQueryChange(query: String) {
        state.update { it.copy(query = query) }

        isSearchScreenRequested.value = query.isNotBlank()
    }

    override fun onFocusChange(isFocused: Boolean) {
        this.isFocused = isFocused
        updateActive()
    }

    override fun onClose() {
        isFocused = false
        isSearchScreenRequested.value = false
        updateActive()
    }

    /** Reports the actual presence of the search screen on the feed stack. */
    fun setSearchScreenShown(isShown: Boolean) {
        // leaving the search with a query still typed (back, close, a collapsed sheet) ends the session;
        // leaving it because the bar was cleared keeps the bar focused for the next query
        if (isSearchScreenShown && !isShown) {
            if (state.value.query.isNotBlank()) isFocused = false
            isSearchScreenRequested.value = false
        }
        isSearchScreenShown = isShown
        updateActive()
    }

    /** Ends the search session along with the feed host that owned it. */
    fun reset() {
        isFocused = false
        isSearchScreenShown = false
        isSearchScreenRequested.value = false
        updateActive()
    }

    private fun updateActive() {
        val isActive = isFocused || isSearchScreenShown
        // deactivation also drops the query, so the next search session starts clean
        state.update { if (isActive) it.copy(isActive = true) else FeedSearchBarUM() }
    }
}