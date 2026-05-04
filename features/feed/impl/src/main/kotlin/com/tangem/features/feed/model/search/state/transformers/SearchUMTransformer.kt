package com.tangem.features.feed.model.search.state.transformers

import com.tangem.features.feed.ui.search.state.SearchUM

internal interface SearchUMTransformer {
    fun transform(prevState: SearchUM): SearchUM
}