package com.tangem.features.feed.model.search.state.transformers

import com.tangem.features.feed.ui.search.state.TokenSelectorContentUM

internal interface TokenSelectorUMTransformer {
    fun transform(prevState: TokenSelectorContentUM): TokenSelectorContentUM
}