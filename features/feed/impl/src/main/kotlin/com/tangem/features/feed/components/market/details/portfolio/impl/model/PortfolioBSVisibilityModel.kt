package com.tangem.features.feed.components.market.details.portfolio.impl.model

/**
 * Model for portfolio bottom sheet visibility
 *
 * @property isAddToPortfolioBSVisible visibility of add to portfolio bottom sheet
 * @property isWalletSelectorBSVisible visibility of wallet selector bottom sheet
 *
[REDACTED_AUTHOR]
 */
internal data class PortfolioBSVisibilityModel(
    val isAddToPortfolioBSVisible: Boolean = false,
    val isWalletSelectorBSVisible: Boolean = false,
)