package com.tangem.features.feed.model.search.state.transformers

import com.tangem.features.feed.ui.search.state.SearchUM

internal class UpdateSearchBarQueryTransformer(
    private val query: String,
) : SearchUMTransformer {

    override fun transform(prevState: SearchUM): SearchUM {
        return prevState.copy(
            searchBar = prevState.searchBar.copy(query = query),
        )
    }
}