package com.tangem.domain.tokens.operations

import arrow.core.NonEmptyList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.isIncludeStakingTotalBalance
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

internal class TokenListFiatBalanceOperations(
    private val currencies: NonEmptyList<CryptoCurrencyStatus>,
    private val isAnyTokenLoading: Boolean,
) {

    @Suppress("LoopWithTooManyJumpStatements")
    fun calculateFiatBalance(): TotalFiatBalance {
        var fiatBalance: TotalFiatBalance = TotalFiatBalance.Loading
        if (isAnyTokenLoading) return fiatBalance

        for (token in currencies) {
            val networkId = token.currency.network.id.value
            val includeStakingBalance = isIncludeStakingTotalBalance(networkId)
            when (val status = token.value) {
                is CryptoCurrencyStatus.Loading -> {
                    fiatBalance = TotalFiatBalance.Loading
                    break
                }
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.MissedDerivation,
                -> {
                    fiatBalance = TotalFiatBalance.Failed
                    break
                }
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                -> {
                    if (BlockchainUtils.isIncludeToBalanceOnError(networkId)) {
                        fiatBalance = recalculateNoAccountBalance(status, fiatBalance)
                    } else {
                        fiatBalance = TotalFiatBalance.Failed
                        break
                    }
                }
                is CryptoCurrencyStatus.NoAccount -> {
                    fiatBalance = recalculateNoAccountBalance(status, fiatBalance)
                }
                is CryptoCurrencyStatus.Loaded -> {
                    fiatBalance = recalculateBalance(status, fiatBalance, includeStakingBalance)
                }
                is CryptoCurrencyStatus.Custom -> {
                    fiatBalance = recalculateBalance(status, fiatBalance, includeStakingBalance)
                }
            }
        }

        return (fiatBalance as? TotalFiatBalance.Loaded)?.copy(
            source = currencies.map { it.value.source }.getResultStatusSource(),
        ) ?: fiatBalance
    }

    private fun recalculateNoAccountBalance(
        status: CryptoCurrencyStatus.Value,
        currentBalance: TotalFiatBalance,
    ): TotalFiatBalance {
        return (currentBalance as? TotalFiatBalance.Loaded)?.copy(isAllAmountsSummarized = false)
            ?: TotalFiatBalance.Loaded(
                amount = BigDecimal.ZERO,
                isAllAmountsSummarized = false,
                source = (status as? CryptoCurrencyStatus.NoAccount)?.source ?: StatusSource.ACTUAL,
            )
    }

    private fun recalculateBalance(
        status: CryptoCurrencyStatus.Loaded,
        currentBalance: TotalFiatBalance,
        includeStakingBalance: Boolean,
    ): TotalFiatBalance {
        return with(currentBalance) {
            val yieldBalance = status.yieldBalance as? YieldBalance.Data
            val stakingBalance = yieldBalance?.getTotalWithRewardsStakingBalance().orZero()
            val fiatStakingBalance = if (includeStakingBalance) {
                status.fiatRate.times(stakingBalance)
            } else {
                BigDecimal.ZERO
            }
            (this as? TotalFiatBalance.Loaded)?.copy(
                amount = this.amount + status.fiatAmount + fiatStakingBalance,
            ) ?: TotalFiatBalance.Loaded(
                amount = status.fiatAmount + fiatStakingBalance,
                isAllAmountsSummarized = true,
                source = status.source,
            )
        }
    }

    private fun recalculateBalance(
        status: CryptoCurrencyStatus.Custom,
        currentBalance: TotalFiatBalance,
        includeStakingBalance: Boolean,
    ): TotalFiatBalance {
        return with(currentBalance) {
            val isTokenAmountCanBeSummarized = status.fiatAmount != null
            val yieldBalance = (status.yieldBalance as? YieldBalance.Data)?.getTotalWithRewardsStakingBalance().orZero()
            val fiatYieldBalance = if (includeStakingBalance) {
                status.fiatRate?.times(yieldBalance).orZero()
            } else {
                BigDecimal.ZERO
            }
            (this as? TotalFiatBalance.Loaded)?.copy(
                amount = this.amount + status.fiatAmount.orZero() + fiatYieldBalance,
                isAllAmountsSummarized = isTokenAmountCanBeSummarized,
            ) ?: TotalFiatBalance.Loaded(
                amount = status.fiatAmount.orZero() + fiatYieldBalance,
                isAllAmountsSummarized = isTokenAmountCanBeSummarized,
                source = StatusSource.ACTUAL,
            )
        }
    }
}