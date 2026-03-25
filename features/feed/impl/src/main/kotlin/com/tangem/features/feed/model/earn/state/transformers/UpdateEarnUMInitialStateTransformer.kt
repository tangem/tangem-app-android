package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.earn.state.EarnUM
import com.tangem.features.feed.ui.feed.state.FeedListSearchBar

internal class UpdateEarnUMInitialStateTransformer(
    private val onBackClick: () -> Unit,
    private val onNetworkFilterClick: () -> Unit,
    private val onTypeFilterClick: () -> Unit,
    private val onScroll: () -> Unit,
    private val onSearchBarClicked: () -> Unit,
) : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(
            onBackClick = onBackClick,
            onNetworkFilterClick = onNetworkFilterClick,
            onTypeFilterClick = onTypeFilterClick,
            onSliderScroll = onScroll,
            feedListSearchBar = FeedListSearchBar(
                onBarClick = onSearchBarClicked,
                placeholderText = resourceReference(id = R.string.markets_search_title_placeholder),
            ),
        )
    }
}