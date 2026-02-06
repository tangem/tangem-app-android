package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.feed.state.FeedListUM

internal class UpdateEarnLoadingStateTransformer : FeedListUMTransformer {

    override fun transform(prevState: FeedListUM): FeedListUM {
        return prevState.copy(earnListUM = EarnListUM.Loading)
    }
}