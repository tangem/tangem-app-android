package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import arrow.core.getOrElse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.analytics.CheckIsWalletToppedUpUseCase
import com.tangem.domain.analytics.model.WalletBalanceState
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.Basic
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import javax.inject.Inject

@ViewModelScoped
internal class TokenListAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val checkIsWalletToppedUpUseCase: CheckIsWalletToppedUpUseCase,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
) {

    private val balanceWasSentMap = mutableMapOf<String, Boolean>()
    private val mutex = Mutex()

    suspend fun send(displayedUiState: WalletState?, userWallet: UserWallet, tokenList: TokenList) {
        if (screenLifecycleProvider.isBackground) return
        if (displayedUiState == null || displayedUiState.pullToRefreshConfig.isRefreshing) return
        if (tokenList.totalFiatBalance is TokenList.FiatBalance.Loading) return

        val currenciesStatuses = getCurrenciesStatuses(tokenList)

        sendBalanceLoadedEventIfNeeded(tokenList.totalFiatBalance, currenciesStatuses)
        sendToppedUpEventIfNeeded(userWallet, tokenList.totalFiatBalance, currenciesStatuses)
        sendUnreachableNetworksEventIfNeeded(currenciesStatuses)
        sendTokenBalancesIfNeeded(currenciesStatuses)
    }

    private fun getCurrenciesStatuses(tokenList: TokenList): List<CryptoCurrencyStatus> = when (tokenList) {
        is TokenList.Empty -> emptyList()
        is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies)
        is TokenList.Ungrouped -> tokenList.currencies
    }

    private fun sendBalanceLoadedEventIfNeeded(
        fiatBalance: TokenList.FiatBalance,
        currenciesStatuses: List<CryptoCurrencyStatus>,
    ) {
        createCardBalanceState(fiatBalance, currenciesStatuses)?.let { balanceState ->
            analyticsEventHandler.send(Basic.BalanceLoaded(balanceState))
        }
    }

    private fun createCardBalanceState(
        fiatBalance: TokenList.FiatBalance,
        currenciesStatuses: List<CryptoCurrencyStatus>,
    ): AnalyticsParam.CardBalanceState? {
        return when (fiatBalance) {
            is TokenList.FiatBalance.Failed -> getCardBalanceState(currenciesStatuses)
            is TokenList.FiatBalance.Loaded -> getCardBalanceState(fiatBalance)
            is TokenList.FiatBalance.Loading -> null
        }
    }

    private fun getCardBalanceState(currenciesStatuses: List<CryptoCurrencyStatus>): AnalyticsParam.CardBalanceState {
        return when {
            currenciesStatuses.isEmpty() -> AnalyticsParam.CardBalanceState.Empty
            currenciesStatuses.any { it.value is CryptoCurrencyStatus.NoQuote } -> {
                AnalyticsParam.CardBalanceState.NoRate
            }
            else -> AnalyticsParam.CardBalanceState.BlockchainError
        }
    }

    private suspend fun sendTokenBalancesIfNeeded(currenciesStatuses: List<CryptoCurrencyStatus>) {
        currenciesStatuses.forEach {
            val status = it.value
            if (status is CryptoCurrencyStatus.Loaded) {
                sendTokenBalancesForSpecificBlockchains(it, status)
            }
        }
    }

    // TODO hotfix/5.7.4 send event for log if tokens from polkadot ecosystem have balance
    private suspend fun sendTokenBalancesForSpecificBlockchains(
        currencyStatus: CryptoCurrencyStatus,
        balanceStatus: CryptoCurrencyStatus.Loaded,
    ) {
        // for now send only for Polkadot ecosystem blockchains
        // later dependency on Blockchain will be removed and use token name
        when (val blockchain = Blockchain.fromNetworkId(currencyStatus.currency.network.backendId)) {
            Blockchain.Polkadot,
            Blockchain.AlephZero,
            Blockchain.Kusama,
            -> {
                if (balanceWasSentMap[blockchain.currency] != true) {
                    val tokenBalance = if (balanceStatus.amount.isZero()) {
                        AnalyticsParam.TokenBalanceState.Empty
                    } else {
                        AnalyticsParam.TokenBalanceState.Full
                    }
                    analyticsEventHandler.send(
                        Basic.TokenBalance(
                            balance = tokenBalance,
                            token = blockchain.currency,
                        ),
                    )
                    mutex.withLock {
                        balanceWasSentMap[blockchain.currency] = true
                    }
                }
            }
            else -> {
                /* no-op */
            }
        }
    }

    private fun getCardBalanceState(fiatBalance: TokenList.FiatBalance.Loaded): AnalyticsParam.CardBalanceState {
        return if (fiatBalance.amount > BigDecimal.ZERO) {
            AnalyticsParam.CardBalanceState.Full
        } else {
            AnalyticsParam.CardBalanceState.Empty
        }
    }

    private suspend fun sendToppedUpEventIfNeeded(
        userWallet: UserWallet,
        fiatBalance: TokenList.FiatBalance,
        currenciesStatuses: List<CryptoCurrencyStatus>,
    ) {
        val balanceState = getWalletBalanceState(fiatBalance) ?: return

        val isWalletToppedUp = checkIsWalletToppedUpUseCase(userWallet.walletId, balanceState)
            .getOrElse { return }

        if (isWalletToppedUp) {
            val walletType = if (userWallet.isMultiCurrency) {
                AnalyticsParam.WalletType.MultiCurrency
            } else {
                // For single currency wallets with token list, e.g. Noodle
                val currency = currenciesStatuses.firstOrNull() ?: return

                AnalyticsParam.WalletType.SingleCurrency(currency.currency.name)
            }

            analyticsEventHandler.send(Basic.WalletToppedUp(userWallet.walletId, walletType))
        }
    }

    private fun getWalletBalanceState(fiatBalance: TokenList.FiatBalance): WalletBalanceState? {
        return when (fiatBalance) {
            is TokenList.FiatBalance.Failed -> WalletBalanceState.Error
            is TokenList.FiatBalance.Loaded -> {
                if (fiatBalance.amount > BigDecimal.ZERO) {
                    WalletBalanceState.ToppedUp
                } else {
                    WalletBalanceState.Empty
                }
            }
            is TokenList.FiatBalance.Loading -> null
        }
    }

    private fun sendUnreachableNetworksEventIfNeeded(currenciesStatuses: List<CryptoCurrencyStatus>) {
        val hasUnreachableCurrencies = currenciesStatuses.any {
            it.value is CryptoCurrencyStatus.Unreachable
        }

        if (hasUnreachableCurrencies) {
            analyticsEventHandler.send(MainScreen.NetworksUnreachable)
        }
    }
}