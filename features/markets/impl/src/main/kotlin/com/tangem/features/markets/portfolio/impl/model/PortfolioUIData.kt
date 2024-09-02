package com.tangem.features.markets.portfolio.impl.model

import com.tangem.domain.wallets.models.UserWalletId

/**
 * Portfolio UI data. Combined data from all UI flows that required to setup portfolio
 *
 * @property portfolioBSVisibilityModel portfolio bottom sheet visibility model
 * @property selectedWalletId           selected wallet id
 * @property walletsWithChangedNetworks wallets with changed networks
 * @property hasMissedDerivations       flag that indicates if user has missed derivations
 *
 * @author Andrew Khokhlov on 29/08/2024
 */
internal data class PortfolioUIData(
    val portfolioBSVisibilityModel: PortfolioBSVisibilityModel,
    val selectedWalletId: UserWalletId?,
    val walletsWithChangedNetworks: Map<UserWalletId, List<String>>,
    val hasMissedDerivations: Boolean,
)
