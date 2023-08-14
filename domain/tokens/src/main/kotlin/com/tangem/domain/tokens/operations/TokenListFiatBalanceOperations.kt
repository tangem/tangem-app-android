package com.tangem.domain.tokens.operations

import arrow.core.NonEmptyList
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import java.math.BigDecimal

internal class TokenListFiatBalanceOperations(
    private val currencies: NonEmptyList<CryptoCurrencyStatus>,
    private val isAnyTokenLoading: Boolean,
) {

    fun calculateFiatBalance(): TokenList.FiatBalance {
        var fiatBalance: TokenList.FiatBalance = TokenList.FiatBalance.Loading
        if (isAnyTokenLoading) return fiatBalance

        for (token in currencies) {
            when (val status = token.value) {
                is CryptoCurrencyStatus.Loading -> {
                    fiatBalance = TokenList.FiatBalance.Loading
                    break
                }
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Unreachable,
                -> {
                    fiatBalance = TokenList.FiatBalance.Failed
                    break
                }
                is CryptoCurrencyStatus.NoAccount -> {
                    fiatBalance = recalculateBalanceWithoutQuote(fiatBalance)
                }
                is CryptoCurrencyStatus.Loaded -> {
                    fiatBalance = recalculateBalance(status, fiatBalance)
                }
                is CryptoCurrencyStatus.Custom -> {
                    fiatBalance = recalculateBalance(status, fiatBalance)
                }
                is CryptoCurrencyStatus.NoQuote -> {
                    fiatBalance = recalculateBalanceWithoutQuote(fiatBalance)
                }
            }
        }

        return fiatBalance
    }

    private fun recalculateBalanceWithoutQuote(currentBalance: TokenList.FiatBalance): TokenList.FiatBalance {
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
        status: CryptoCurrencyStatus.Loaded,
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
        status: CryptoCurrencyStatus.Custom,
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
