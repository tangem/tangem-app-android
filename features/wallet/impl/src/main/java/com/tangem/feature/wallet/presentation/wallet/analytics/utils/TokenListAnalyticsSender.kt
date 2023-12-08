package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.analytics.CheckIsWalletToppedUpUseCase
import com.tangem.domain.analytics.model.WalletBalanceState
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import dagger.hilt.android.scopes.ViewModelScoped
import java.math.BigDecimal
import javax.inject.Inject

@ViewModelScoped
internal class TokenListAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val checkIsWalletToppedUpUseCase: CheckIsWalletToppedUpUseCase,
) {

    suspend fun send(userWallet: UserWallet, maybeTokenList: Either<TokenListError, TokenList>) {
        val tokenList = (maybeTokenList as? Either.Right)?.value ?: return

        sendBalanceLoadedEventIfNeeded(tokenList)
        sendToppedUpEventIfNeeded(tokenList, userWallet)
    }

    private fun sendBalanceLoadedEventIfNeeded(tokenList: TokenList) {
        createCardBalanceState(tokenList)?.let { balanceState ->
            analyticsEventHandler.send(event = WalletScreenAnalyticsEvent.Basic.BalanceLoaded(balanceState))
        }
    }

    private suspend fun sendToppedUpEventIfNeeded(tokenList: TokenList, userWallet: UserWallet) {
        val balanceState = tokenList.toWalletBalanceState() ?: return

        val isWalletToppedUp = checkIsWalletToppedUpUseCase(userWallet.walletId, balanceState)
            .getOrElse { return }

        if (isWalletToppedUp) {
            val walletType = if (userWallet.isMultiCurrency) {
                AnalyticsParam.WalletType.MultiCurrency
            } else {
                // For single currency wallets with token list, e.g. Noodle
                val currency = when (tokenList) {
                    is TokenList.Empty -> return
                    is TokenList.GroupedByNetwork -> tokenList.groups.first().currencies.first()
                    is TokenList.Ungrouped -> tokenList.currencies.first()
                }

                AnalyticsParam.WalletType.SingleCurrency(currency.currency.name)
            }

            analyticsEventHandler.send(WalletScreenAnalyticsEvent.Basic.WalletToppedUp(userWallet.walletId, walletType))
        }
    }

    private fun createCardBalanceState(tokenList: TokenList): AnalyticsParam.CardBalanceState? {
        return when (val fiatBalance = tokenList.totalFiatBalance) {
            is TokenList.FiatBalance.Failed -> fiatBalance.toCardBalanceState(tokenList)
            is TokenList.FiatBalance.Loaded -> fiatBalance.toCardBalanceState()
            is TokenList.FiatBalance.Loading -> null
        }
    }

    @Suppress("UnusedReceiverParameter")
    private fun TokenList.FiatBalance.Failed.toCardBalanceState(tokenList: TokenList): AnalyticsParam.CardBalanceState {
        val currenciesStatuses = when (tokenList) {
            is TokenList.Empty -> emptyList()
            is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies)
            is TokenList.Ungrouped -> tokenList.currencies
        }

        return when {
            currenciesStatuses.isEmpty() -> AnalyticsParam.CardBalanceState.Empty
            currenciesStatuses.any { it.value is CryptoCurrencyStatus.NoQuote } -> {
                AnalyticsParam.CardBalanceState.NoRate
            }
            else -> AnalyticsParam.CardBalanceState.BlockchainError
        }
    }

    private fun TokenList.FiatBalance.Loaded.toCardBalanceState(): AnalyticsParam.CardBalanceState {
        return if (amount > BigDecimal.ZERO) {
            AnalyticsParam.CardBalanceState.Full
        } else {
            AnalyticsParam.CardBalanceState.Empty
        }
    }

    private fun TokenList.toWalletBalanceState(): WalletBalanceState? {
        return when (val balance = totalFiatBalance) {
            is TokenList.FiatBalance.Failed -> WalletBalanceState.Error
            is TokenList.FiatBalance.Loaded -> {
                if (balance.amount > BigDecimal.ZERO) {
                    WalletBalanceState.ToppedUp
                } else {
                    WalletBalanceState.Empty
                }
            }
            is TokenList.FiatBalance.Loading -> null
        }
    }
}