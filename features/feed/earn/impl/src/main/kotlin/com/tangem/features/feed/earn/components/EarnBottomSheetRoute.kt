package com.tangem.features.feed.earn.components

import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager

internal sealed interface EarnBottomSheetRoute {

    data class AddToPortfolio(
        val source: String,
        val manager: AddToPortfolioManager,
    ) : EarnBottomSheetRoute

    data class NetworkFilter(val params: EarnNetworkFilterComponent.Params) : EarnBottomSheetRoute

    data class TypeFilter(val params: EarnTypeFilterComponent.Params) : EarnBottomSheetRoute
}