package com.tangem.features.markets.portfolio.impl.loader

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Portfolio data. Combined data from all flows that required to setup portfolio
 *
 * @property walletsWithCurrencies wallets with crypto currency statuses
 * @property appCurrency                 app currency
 * @property isBalanceHidden             flag that indicates if balance should be hidden
 * @property walletsWithBalance          wallets with total balance
 *
[REDACTED_AUTHOR]
 */
internal data class PortfolioData(
    val walletsWithCurrencies: Map<UserWallet, List<CryptoCurrencyData>>,
    val appCurrency: AppCurrency,
    val isBalanceHidden: Boolean,
    val walletsWithBalance: Map<UserWalletId, Lce<TokenListError, TotalFiatBalance>>,
) {
    data class CryptoCurrencyData(
        val userWallet: UserWallet,
        val status: CryptoCurrencyStatus,
        val actions: List<TokenActionsState.ActionState>,
    )
}