package com.tangem.features.account

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.error.TokenListError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PortfolioFetcher {

    val data: Flow<Data>

    val mode: StateFlow<Mode>
    fun updateMode(mode: Mode)

    data class Data(
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
        val balances: Map<UserWallet, PortfolioBalance>,
    )

    data class PortfolioBalance(
        val walletBalance: Lce<TokenListError, TotalFiatBalance>,
        val accountsBalance: AccountStatusList,
    ) {
        val userWalletId: UserWalletId get() = accountsBalance.userWalletId
    }

    sealed interface Mode {
        data class All(val onlyMultiCurrency: Boolean) : Mode
        data class Wallet(val walletId: UserWalletId) : Mode
    }

    interface Factory {
        fun create(mode: Mode, scope: CoroutineScope): PortfolioFetcher
    }
}