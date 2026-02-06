package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.features.feed.ui.feed.state.FeedListUM

internal interface FeedListUMTransformer {
    fun transform(prevState: FeedListUM): FeedListUM
}