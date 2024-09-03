package com.tangem.features.markets.portfolio.impl.model

import com.tangem.domain.wallets.models.UserWalletId

/**
 * Portfolio UI data. Combined data from all UI flows that required to setup portfolio
 *
 * @property portfolioBSVisibilityModel portfolio bottom sheet visibility model
 * @property selectedWalletId           selected wallet id
 * @property addToPortfolioData         add to portfolio data
 * @property hasMissedDerivations       flag that indicates if user has missed derivations
 *
[REDACTED_AUTHOR]
 */
internal data class PortfolioUIData(
    val portfolioBSVisibilityModel: PortfolioBSVisibilityModel,
    val selectedWalletId: UserWalletId?,
    val addToPortfolioData: AddToPortfolioManager.AddToPortfolioData,
    val hasMissedDerivations: Boolean,
)