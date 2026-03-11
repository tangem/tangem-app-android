package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.features.feed.ui.earn.state.EarnUM

internal class UpdateEarnUMInitialStateTransformer(
    private val onBackClick: () -> Unit,
    private val onNetworkFilterClick: () -> Unit,
    private val onTypeFilterClick: () -> Unit,
    private val onScroll: () -> Unit,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(
            onBackClick = onBackClick,
            onNetworkFilterClick = onNetworkFilterClick,
            onTypeFilterClick = onTypeFilterClick,
            onSliderScroll = onScroll,
        )
    }
}