package com.tangem.features.feed.components.feed

import com.tangem.features.feed.components.earn.EarnNetworkFilterComponent
import com.tangem.features.feed.components.earn.EarnTypeFilterComponent

internal sealed interface FeedBottomSheetRoute {

    data class AddToPortfolio(
        val source: String,
    ) : FeedBottomSheetRoute

    data class NetworkFilter(val params: EarnNetworkFilterComponent.Params) : FeedBottomSheetRoute

    data class TypeFilter(val params: EarnTypeFilterComponent.Params) : FeedBottomSheetRoute
}