package com.tangem.domain.tokens.utils

import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class TokenListFiatBalanceOperations(
    private val tokens: Set<TokenStatus>,
    private val isAnyTokenLoading: Boolean,
    private val dispatcher: CoroutineDispatcherProvider,
) {

    suspend fun calculateFiatBalance(): TokenList.FiatBalance {
        return withContext(dispatcher.single) {
            var fiatBalance: TokenList.FiatBalance = TokenList.FiatBalance.Loading
            if (tokens.isEmpty() || isAnyTokenLoading) return@withContext fiatBalance

            for (token in tokens) {
                when (val status = token.value) {
                    is TokenStatus.Loading -> {
                        fiatBalance = TokenList.FiatBalance.Loading
                        break
                    }
                    is TokenStatus.MissedDerivation,
                    is TokenStatus.Unreachable,
                    -> {
                        fiatBalance = TokenList.FiatBalance.Failed
                        break
                    }
                    is TokenStatus.NoAccount -> {
                        fiatBalance = recalculateBalanceForNoAccountStatus(fiatBalance)
                    }
                    is TokenStatus.Loaded -> {
                        fiatBalance = recalculateBalance(status, fiatBalance)
                    }
                    is TokenStatus.Custom -> {
                        fiatBalance = recalculateBalance(status, fiatBalance)
                    }
                }
            }

            fiatBalance
        }
    }
    private fun recalculateBalanceForNoAccountStatus(currentBalance: TokenList.FiatBalance): TokenList.FiatBalance {
        return with(currentBalance) {
            (this as? TokenList.FiatBalance.Loaded)?.copy(
                isAllAmountsSummarized = false,
            ) ?: TokenList.FiatBalance.Loaded(
                amount = BigDecimal.ZERO,
                isAllAmountsSummarized = false,
            )
        }
    }

    private fun recalculateBalance(
        status: TokenStatus.Loaded,
        currentBalance: TokenList.FiatBalance,
    ): TokenList.FiatBalance {
        return with(currentBalance) {
            (this as? TokenList.FiatBalance.Loaded)?.copy(
                amount = this.amount + status.fiatAmount,
            ) ?: TokenList.FiatBalance.Loaded(
                amount = status.fiatAmount,
                isAllAmountsSummarized = true,
            )
        }
    }

    private fun recalculateBalance(
        status: TokenStatus.Custom,
        currentBalance: TokenList.FiatBalance,
    ): TokenList.FiatBalance {
        return with(currentBalance) {
            val isTokenAmountCanBeSummarized = status.fiatAmount != null

            (this as? TokenList.FiatBalance.Loaded)?.copy(
                amount = this.amount + (status.fiatAmount ?: BigDecimal.ZERO),
                isAllAmountsSummarized = isTokenAmountCanBeSummarized,
            ) ?: TokenList.FiatBalance.Loaded(
                amount = status.fiatAmount ?: BigDecimal.ZERO,
                isAllAmountsSummarized = isTokenAmountCanBeSummarized,
            )
        }
    }
}
