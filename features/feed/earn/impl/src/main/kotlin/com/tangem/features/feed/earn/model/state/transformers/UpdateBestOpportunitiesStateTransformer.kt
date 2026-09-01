package com.tangem.features.feed.earn.model.state.transformers

import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.utils.transformer.Transformer

internal class UpdateBestOpportunitiesStateTransformer(
    private val newState: EarnBestOpportunitiesUM,
) : Transformer<EarnFeedTabUM> {

    override fun transform(prevState: EarnFeedTabUM): EarnFeedTabUM {
        return prevState.copy(bestOpportunities = newState)
    }
}