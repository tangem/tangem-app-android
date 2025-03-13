package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import arrow.core.getOrElse
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.analytics.CheckIsWalletToppedUpUseCase
import com.tangem.domain.analytics.model.WalletBalanceState
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.Basic
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import javax.inject.Inject

@ModelScoped
internal class TokenListAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val checkIsWalletToppedUpUseCase: CheckIsWalletToppedUpUseCase,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
) {

    private val balanceWasSentMap = mutableMapOf<String, Boolean>()
    private val mutex = Mutex()
    private val loadingTraces = mutableMapOf<UserWalletId, Trace>()

    suspend fun send(displayedUiState: WalletState?, userWallet: UserWallet, tokenList: TokenList) {
        if (screenLifecycleProvider.isBackgroundState.value) return
        if (displayedUiState == null || displayedUiState.pullToRefreshConfig.isRefreshing) return

        if (tokenList.totalFiatBalance is TotalFiatBalance.Loading) {
            startLoadingTraceIfNeeded(userWallet.walletId, tokenList)
            return
        }

        if (isTerminalState(tokenList.totalFiatBalance)) {
            stopLoadingTraceIfNeeded(userWallet.walletId, tokenList.totalFiatBalance)
        }

        val currenciesStatuses = tokenList.flattenCurrencies()

        sendBalanceLoadedEventIfNeeded(tokenList.totalFiatBalance, currenciesStatuses)
        sendToppedUpEventIfNeeded(userWallet, tokenList.totalFiatBalance, currenciesStatuses)
        sendUnreachableNetworksEventIfNeeded(currenciesStatuses)
        sendTokenBalancesIfNeeded(currenciesStatuses)
    }

    private suspend fun startLoadingTraceIfNeeded(userWalletId: UserWalletId, tokenList: TokenList) {
        mutex.withLock {
            if (!loadingTraces.containsKey(userWalletId)) {
                val trace = FirebasePerformance.getInstance().newTrace(BALANCE_LOADED_TRACE_NAME)
                trace.start()
                trace.putAttribute(TOKENS_COUNT, tokenList.flattenCurrencies().size.toString())
                loadingTraces[userWalletId] = trace
            }
        }
    }

    private suspend fun stopLoadingTraceIfNeeded(userWalletId: UserWalletId, totalFiatBalance: TotalFiatBalance) {
        mutex.withLock {
            loadingTraces[userWalletId]?.apply {
                when (totalFiatBalance) {
                    is TotalFiatBalance.Loaded -> putAttribute(HAS_ERROR, "No")
                    is TotalFiatBalance.Failed -> putAttribute(HAS_ERROR, "Yes")
                    else -> { /* Intentionally do nothing */ }
                }
                stop()
                loadingTraces.remove(userWalletId)
            }
        }
    }

    private fun isTerminalState(balance: TotalFiatBalance): Boolean {
        return balance is TotalFiatBalance.Failed || balance is TotalFiatBalance.Loaded
    }

    private fun sendBalanceLoadedEventIfNeeded(
        fiatBalance: TotalFiatBalance,
        currenciesStatuses: List<CryptoCurrencyStatus>,
    ) {
        createCardBalanceState(fiatBalance, currenciesStatuses)?.let { balanceState ->
            analyticsEventHandler.send(
                Basic.BalanceLoaded(
                    balance = balanceState,
                    tokensCount = currenciesStatuses.size,
                ),
            )
        }
    }

    private fun createCardBalanceState(
        fiatBalance: TotalFiatBalance,
        currenciesStatuses: List<CryptoCurrencyStatus>,
    ): AnalyticsParam.CardBalanceState? {
        return when (fiatBalance) {
            is TotalFiatBalance.Failed -> getCardBalanceState(currenciesStatuses)
            is TotalFiatBalance.Loaded -> getCardBalanceState(fiatBalance)
            is TotalFiatBalance.Loading -> null
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

    private fun getCardBalanceState(fiatBalance: TotalFiatBalance.Loaded): AnalyticsParam.CardBalanceState {
        return if (fiatBalance.amount > BigDecimal.ZERO) {
            AnalyticsParam.CardBalanceState.Full
        } else {
            AnalyticsParam.CardBalanceState.Empty
        }
    }

    private suspend fun sendToppedUpEventIfNeeded(
        userWallet: UserWallet,
        fiatBalance: TotalFiatBalance,
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

    private fun getWalletBalanceState(fiatBalance: TotalFiatBalance): WalletBalanceState? {
        return when (fiatBalance) {
            is TotalFiatBalance.Failed -> WalletBalanceState.Error
            is TotalFiatBalance.Loaded -> {
                if (fiatBalance.amount > BigDecimal.ZERO) {
                    WalletBalanceState.ToppedUp
                } else {
                    WalletBalanceState.Empty
                }
            }
            is TotalFiatBalance.Loading -> null
        }
    }

    private fun sendUnreachableNetworksEventIfNeeded(currenciesStatuses: List<CryptoCurrencyStatus>) {
        val unreachableCurrencies = currenciesStatuses
            .filter { it.value is CryptoCurrencyStatus.Unreachable }
            .map { it.currency.symbol }

        if (unreachableCurrencies.isNotEmpty()) {
            analyticsEventHandler.send(MainScreen.NetworksUnreachable(unreachableCurrencies))
        }
    }

    companion object {
        const val BALANCE_LOADED_TRACE_NAME = "Total_balance_loaded"
        const val HAS_ERROR = "has_error"
        const val TOKENS_COUNT = "tokens_count"
    }
}