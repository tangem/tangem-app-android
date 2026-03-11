package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.features.feed.ui.earn.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class UpdateBestOpportunitiesStateTransformer(
    private val newState: EarnBestOpportunitiesUM,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(bestOpportunities = newState)
    }
}