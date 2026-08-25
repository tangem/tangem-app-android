package com.tangem.features.feed.earn.model.state.transformers

import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.utils.transformer.Transformer

internal class UpdateEarnFeedTabUMInitialStateTransformer(
    private val onScroll: () -> Unit,
) : Transformer<EarnFeedTabUM> {

    override fun transform(prevState: EarnFeedTabUM): EarnFeedTabUM = prevState.copy(onSliderScroll = onScroll)
}