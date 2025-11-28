package com.tangem.features.account

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * How to use see [PortfolioSelectorComponent]
 */
interface PortfolioFetcher {

    val data: Flow<Data>

    val mode: StateFlow<Mode>
    fun updateMode(mode: Mode)

    data class Data(
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
        val balances: Map<UserWalletId, PortfolioBalance>,
    ) {

        val isSingleChoice: Boolean = balances.values
            .map { it.accountsBalance.accountStatuses }
            .flatten().size == 1

        fun isSingleChoice(walletId: UserWalletId): Boolean = balances[walletId]
            ?.accountsBalance
            ?.accountStatuses
            ?.size == 1
    }

    data class PortfolioBalance(
        val userWallet: UserWallet,
        val accountsBalance: AccountStatusList,
    ) {
        val walletBalance get() = accountsBalance.totalFiatBalance
        val userWalletId: UserWalletId get() = userWallet.walletId
    }

    sealed interface Mode {
        data class All(val onlyMultiCurrency: Boolean) : Mode
        data class Wallet(val walletId: UserWalletId) : Mode
    }

    /**
     * @param[mode] supports runtime change [PortfolioFetcher.updateMode]
     */
    interface Factory {
        fun create(mode: Mode, scope: CoroutineScope): PortfolioFetcher
    }
}