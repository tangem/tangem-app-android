package com.tangem.features.feed.components.v2

import com.tangem.features.feed.search.FeedSearchBarController
import com.tangem.features.feed.search.FeedSearchBarUM
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultFeedSearchBarController @Inject constructor() : FeedSearchBarController {

    override val state: StateFlow<FeedSearchBarUM>
        field = MutableStateFlow(FeedSearchBarUM())

    override fun onQueryChange(query: String) {
        state.update { it.copy(query = query) }
    }

    override fun onActiveChange(isActive: Boolean) {
        // deactivation also drops the query, so the next search session starts clean
        state.update { if (isActive) it.copy(isActive = true) else FeedSearchBarUM() }
    }
}