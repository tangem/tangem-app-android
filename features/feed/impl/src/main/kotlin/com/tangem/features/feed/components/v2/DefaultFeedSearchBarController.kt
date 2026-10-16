package com.tangem.features.feed.components.v2

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

    /** Open (`true`) / close (`false`) requests for the search screen, collected by the feed host. */
    val searchScreenRequests: SharedFlow<Boolean>
        field = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    override val submits: SharedFlow<Unit>
        field = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var isFocused = false
    private var isSearchScreenShown = false

    override fun onQueryChange(query: String) {
        state.update { it.copy(query = query) }

        if (query.isNotBlank() && !isSearchScreenShown) {
            searchScreenRequests.tryEmit(true)
        }
    }

    override fun onFocusChange(isFocused: Boolean) {
        this.isFocused = isFocused
        updateActive()
    }

    override fun onClose() {
        isFocused = false
        searchScreenRequests.tryEmit(false)
        updateActive()
    }

    override fun onSubmit() {
        submits.tryEmit(Unit)
    }

    /** Reports the actual presence of the search screen on the feed stack. */
    fun setSearchScreenShown(isShown: Boolean) {
        // leaving the search ends the session, even if the bar still holds focus
        if (isSearchScreenShown && !isShown) isFocused = false
        isSearchScreenShown = isShown
        updateActive()
    }

    /** Ends the search session along with the feed host that owned it. */
    fun reset() {
        isFocused = false
        isSearchScreenShown = false
        updateActive()
    }

    private fun updateActive() {
        val isActive = isFocused || isSearchScreenShown
        // deactivation also drops the query, so the next search session starts clean
        state.update { if (isActive) it.copy(isActive = true) else FeedSearchBarUM() }
    }
}