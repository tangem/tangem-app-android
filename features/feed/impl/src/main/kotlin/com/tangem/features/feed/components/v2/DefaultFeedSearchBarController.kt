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

    /** Open (`true`) / close (`false`) requests from the search bar, collected by the feed host. */
    val activationRequests: SharedFlow<Boolean>
        field = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    override val submits: SharedFlow<Unit>
        field = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onQueryChange(query: String) {
        state.update { it.copy(query = query) }
    }

    override fun onSubmit() {
        submits.tryEmit(Unit)
    }

    override fun onActiveChange(isActive: Boolean) {
        activationRequests.tryEmit(isActive)
    }

    /** Reports the actual presence of the search screen on the feed stack. */
    fun setActive(isActive: Boolean) {
        // deactivation also drops the query, so the next search session starts clean
        state.update { if (isActive) it.copy(isActive = true) else FeedSearchBarUM() }
    }
}