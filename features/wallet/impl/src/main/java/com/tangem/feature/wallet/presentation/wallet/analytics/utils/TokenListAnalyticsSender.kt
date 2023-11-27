package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import arrow.core.Either
import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import dagger.hilt.android.scopes.ViewModelScoped
import java.math.BigDecimal
import javax.inject.Inject

@ViewModelScoped
internal class TokenListAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun send(maybeTokenList: Either<TokenListError, TokenList>) {
        val tokenList = (maybeTokenList as? Either.Right)?.value ?: return

        createCardBalanceState(tokenList)?.let {
            analyticsEventHandler.send(event = WalletScreenAnalyticsEvent.Basic.BalanceLoaded(balance = it))
        }
    }

    private fun createCardBalanceState(tokenList: TokenList): AnalyticsParam.CardBalanceState? {
        return when (val fiatBalance = tokenList.totalFiatBalance) {
            is TokenList.FiatBalance.Failed -> fiatBalance.toCardBalanceState(tokenList)
            is TokenList.FiatBalance.Loaded -> fiatBalance.toCardBalanceState()
            TokenList.FiatBalance.Loading -> null
        }
    }

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

    private fun TokenList.FiatBalance.Loaded.toCardBalanceState(): AnalyticsParam.CardBalanceState? {
        return if (amount > BigDecimal.ZERO) {
            AnalyticsParam.CardBalanceState.Full
        } else if (amount.isZero()) {
            AnalyticsParam.CardBalanceState.Empty
        } else {
            null
        }
    }
}
