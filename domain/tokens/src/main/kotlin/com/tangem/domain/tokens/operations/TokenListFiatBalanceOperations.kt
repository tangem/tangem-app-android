package com.tangem.domain.tokens.operations

import arrow.core.NonEmptyList
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.utils.extensions.orZero
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
            val stakingBalance = (status.yieldBalance as? YieldBalance.Data)?.getTotalStakingBalance().orZero()
            val fiatStakingBalance = status.fiatRate.times(stakingBalance)

            (this as? TotalFiatBalance.Loaded)?.copy(
                amount = this.amount + status.fiatAmount + fiatStakingBalance,
            ) ?: TotalFiatBalance.Loaded(
                amount = status.fiatAmount + fiatStakingBalance,
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
            val yieldBalance = (status.yieldBalance as? YieldBalance.Data)?.getTotalStakingBalance().orZero()
            val fiatYieldBalance = status.fiatRate?.times(yieldBalance).orZero()
            (this as? TotalFiatBalance.Loaded)?.copy(
                amount = this.amount + status.fiatAmount.orZero() + fiatYieldBalance,
                isAllAmountsSummarized = isTokenAmountCanBeSummarized,
            ) ?: TotalFiatBalance.Loaded(
                amount = status.fiatAmount.orZero() + fiatYieldBalance,
                isAllAmountsSummarized = isTokenAmountCanBeSummarized,
            )
        }
    }
}
