package com.tangem.features.feed.model.search.state.transformers

import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM

internal interface TokenSelectorUMTransformer {
    fun transform(prevState: TokenSelectorContentUM): TokenSelectorContentUM
}