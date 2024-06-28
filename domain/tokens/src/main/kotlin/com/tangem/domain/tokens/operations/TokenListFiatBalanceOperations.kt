package com.tangem.domain.tokens.operations

import arrow.core.NonEmptyList
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TotalFiatBalance
import java.math.BigDecimal

internal class TokenListFiatBalanceOperations(
    private val currencies: NonEmptyList<CryptoCurrencyStatus>,
    private val isAnyTokenLoading: Boolean,
) {

    fun calculateFiatBalance(): TotalFiatBalance {
        var fiatBalance: TotalFiatBalance = TotalFiatBalance.Loading
        if (isAnyTokenLoading) return fiatBalance

        for (token in currencies) {
            when (val status = token.value) {
                is CryptoCurrencyStatus.Loading -> {
                    fiatBalance = TotalFiatBalance.Loading
                    break
                }
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                is CryptoCurrencyStatus.NoQuote,
                -> {
                    fiatBalance = TotalFiatBalance.Failed
                    break
                }
                is CryptoCurrencyStatus.NoAccount -> {
                    fiatBalance = recalculateNoAccountBalance(fiatBalance)
                }
                is CryptoCurrencyStatus.Loaded -> {
                    fiatBalance = recalculateBalance(status, fiatBalance)
                }
                is CryptoCurrencyStatus.Custom -> {
                    fiatBalance = recalculateBalance(status, fiatBalance)
                }
            }
        }

        return fiatBalance
    }

    private fun recalculateNoAccountBalance(currentBalance: TotalFiatBalance): TotalFiatBalance {
        return (currentBalance as? TotalFiatBalance.Loaded)?.copy(isAllAmountsSummarized = false)
            ?: TotalFiatBalance.Loaded(
                amount = BigDecimal.ZERO,
                isAllAmountsSummarized = false,
            )
    }

    private fun recalculateBalance(
        status: CryptoCurrencyStatus.Loaded,
        currentBalance: TotalFiatBalance,
    ): TotalFiatBalance {
        return with(currentBalance) {
            (this as? TotalFiatBalance.Loaded)?.copy(
                amount = this.amount + status.fiatAmount,
            ) ?: TotalFiatBalance.Loaded(
                amount = status.fiatAmount,
                isAllAmountsSummarized = true,
            )
        }
    }

    private fun recalculateBalance(
        status: CryptoCurrencyStatus.Custom,
        currentBalance: TotalFiatBalance,
    ): TotalFiatBalance {
        return with(currentBalance) {
            val isTokenAmountCanBeSummarized = status.fiatAmount != null

            (this as? TotalFiatBalance.Loaded)?.copy(
                amount = this.amount + (status.fiatAmount ?: BigDecimal.ZERO),
                isAllAmountsSummarized = isTokenAmountCanBeSummarized,
            ) ?: TotalFiatBalance.Loaded(
                amount = status.fiatAmount ?: BigDecimal.ZERO,
                isAllAmountsSummarized = isTokenAmountCanBeSummarized,
            )
        }
    }
}