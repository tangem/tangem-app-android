package com.tangem.features.feed.components.feed

import com.tangem.features.feed.components.earn.EarnNetworkFilterComponent
import com.tangem.features.feed.components.earn.EarnTypeFilterComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent

internal sealed interface FeedBottomSheetRoute {

    data class AddToPortfolio(
        val tokenToAdd: AddToPortfolioPreselectedDataComponent.TokenToAdd,
        val source: String,
    ) : FeedBottomSheetRoute

    data class NetworkFilter(val params: EarnNetworkFilterComponent.Params) : FeedBottomSheetRoute

    data class TypeFilter(val params: EarnTypeFilterComponent.Params) : FeedBottomSheetRoute
}