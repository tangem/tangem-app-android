package com.tangem.features.feed.earn.model.state.transformers

import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM

internal interface EarnFeedTabUMTransformer {
    fun transform(prevState: EarnFeedTabUM): EarnFeedTabUM
}