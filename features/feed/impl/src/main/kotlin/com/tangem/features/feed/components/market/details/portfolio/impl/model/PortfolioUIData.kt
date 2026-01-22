package com.tangem.features.feed.components.market.details.portfolio.impl.model

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Portfolio UI data. Combined data from all UI flows that required to setup portfolio
 *
 * @property portfolioBSVisibilityModel portfolio bottom sheet visibility model
 * @property selectedWalletId           selected wallet id
 * @property addToPortfolioData         add to portfolio data
 * @property isNeededColdWalletInteraction  flag that indicates if user has missed derivations and has a cold wallet
 *
[REDACTED_AUTHOR]
 */
internal data class PortfolioUIData(
    val portfolioBSVisibilityModel: PortfolioBSVisibilityModel,
    val selectedWalletId: UserWalletId?,
    val addToPortfolioData: AddToPortfolioManager.AddToPortfolioData,
    val isNeededColdWalletInteraction: Boolean,
)