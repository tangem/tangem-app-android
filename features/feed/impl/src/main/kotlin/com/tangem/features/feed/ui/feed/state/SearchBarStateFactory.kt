package com.tangem.features.feed.ui.feed.state

import com.tangem.utils.Provider

internal class SearchBarStateFactory(
    private val currentStateProvider: Provider<FeedListUM>,
    private val onStateUpdate: (FeedListUM) -> Unit,
) {

    val searchQuery: String
        get() = currentStateProvider().searchBar.query

    fun onSearchQueryChange(query: String) {
        val currentState = currentStateProvider()
        onStateUpdate(
            currentState.copy(
                searchBar = currentState.searchBar.copy(
                    query = query,
                    isActive = query.isNotEmpty(),
                ),
            ),
        )
    }
}