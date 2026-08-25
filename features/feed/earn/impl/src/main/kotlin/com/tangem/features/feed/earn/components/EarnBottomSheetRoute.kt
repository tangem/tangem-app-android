package com.tangem.features.feed.earn.components

internal sealed interface EarnBottomSheetRoute {

    data class AddToPortfolio(
        val source: String,
    ) : EarnBottomSheetRoute
}