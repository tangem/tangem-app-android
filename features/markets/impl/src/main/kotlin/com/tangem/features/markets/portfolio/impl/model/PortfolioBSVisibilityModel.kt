package com.tangem.features.markets.portfolio.impl.model

/**
 * Model for portfolio bottom sheet visibility
 *
 * @property addToPortfolioBSVisibility visibility of add to portfolio bottom sheet
 * @property walletSelectorBSVisibility visibility of wallet selector bottom sheet
 *
[REDACTED_AUTHOR]
 */
internal data class PortfolioBSVisibilityModel(
    val addToPortfolioBSVisibility: Boolean = false,
    val walletSelectorBSVisibility: Boolean = false,
)