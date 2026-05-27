package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.commonfeatures.impl.R

internal data class AddToPortfolioRouteUiSpec(
    val title: TextReference,
    val isScrollable: Boolean,
    val shouldApplyHorizontalPadding: Boolean,
    val footer: AddToPortfolioFooterKind,
)

internal enum class AddToPortfolioFooterKind {
    None,
    Cancel,
    UserPortfolioAdd,
}

internal fun AddToPortfolioRoutes.uiSpec(): AddToPortfolioRouteUiSpec = when (this) {
    AddToPortfolioRoutes.AddToken -> AddToPortfolioRouteUiSpec(
        title = resourceReference(R.string.common_add_token),
        isScrollable = true,
        shouldApplyHorizontalPadding = true,
        footer = AddToPortfolioFooterKind.None,
    )
    is AddToPortfolioRoutes.NetworkSelector -> AddToPortfolioRouteUiSpec(
        title = resourceReference(R.string.common_add_token),
        isScrollable = true,
        shouldApplyHorizontalPadding = true,
        footer = AddToPortfolioFooterKind.Cancel,
    )
    AddToPortfolioRoutes.PortfolioSelector -> AddToPortfolioRouteUiSpec(
        title = resourceReference(R.string.common_add_token),
        isScrollable = false,
        shouldApplyHorizontalPadding = true,
        footer = AddToPortfolioFooterKind.Cancel,
    )
    AddToPortfolioRoutes.UserPortfolio -> AddToPortfolioRouteUiSpec(
        title = resourceReference(R.string.markets_portfolio_block_title),
        isScrollable = false,
        shouldApplyHorizontalPadding = false,
        footer = AddToPortfolioFooterKind.UserPortfolioAdd,
    )
    AddToPortfolioRoutes.TokenActions -> AddToPortfolioRouteUiSpec(
        title = resourceReference(R.string.common_get_token),
        isScrollable = true,
        shouldApplyHorizontalPadding = true,
        footer = AddToPortfolioFooterKind.None,
    )
    AddToPortfolioRoutes.Empty -> AddToPortfolioRouteUiSpec(
        title = TextReference.EMPTY,
        isScrollable = false,
        shouldApplyHorizontalPadding = false,
        footer = AddToPortfolioFooterKind.None,
    )
}