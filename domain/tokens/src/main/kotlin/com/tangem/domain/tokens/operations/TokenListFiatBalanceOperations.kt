package com.tangem.domain.tokens.operations

import arrow.core.NonEmptyList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.staking.utils.getTotalWithRewardsStakingBalance
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.extensions.orZero
import com.tangem.utils.savableContext
import java.math.BigDecimal

internal class TokenListFiatBalanceOperations(
    private val currencies: NonEmptyList<CryptoCurrencyStatus>,
    private val isAnyTokenLoading: Boolean,
) {

    @Suppress("LoopWithTooManyJumpStatements")
    fun calculateFiatBalance(): TotalFiatBalance = savableContext<TotalFiatBalance>(TotalFiatBalance.Loading) {
        if (isAnyTokenLoading) return@savableContext

        for (token in currencies) {
            val blockchainId = token.currency.network.rawId
            when (val status = token.value) {
                is CryptoCurrencyStatus.Loading -> {
                    update(value = TotalFiatBalance.Loading)
                    break
                }
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.MissedDerivation,
                -> {
                    update(value = TotalFiatBalance.Failed)
                    break
                }
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                -> {
                    if (BlockchainUtils.isIncludeToBalanceOnError(blockchainId)) {
                        update {
                            recalculateNoAccountBalance(status = status, currentBalance = it)
                        }
                    } else {
                        update(value = TotalFiatBalance.Failed)
                        break
                    }
                }
                is CryptoCurrencyStatus.NoAccount -> {
                    update {
                        recalculateNoAccountBalance(status = status, currentBalance = it)
                    }
                }
                is CryptoCurrencyStatus.Loaded -> {
                    update {
                        recalculateBalance(status = status, currentBalance = it, blockchainId = blockchainId)
                    }
                }
                is CryptoCurrencyStatus.Custom -> {
                    update {
                        recalculateBalance(status = status, currentBalance = it, blockchainId = blockchainId)
                    }
                }
            }
        }

        update { balance ->
            (balance as? TotalFiatBalance.Loaded)?.copy(
                source = currencies.map { it.value.sources.total }.getResultStatusSource(),
            ) ?: balance
        }
    }

    private fun recalculateNoAccountBalance(
        status: CryptoCurrencyStatus.Value,
        currentBalance: TotalFiatBalance,
    ): TotalFiatBalance {
        return currentBalance as? TotalFiatBalance.Loaded
            ?: TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = status.sources.total)
    }

    private fun recalculateBalance(
        status: CryptoCurrencyStatus.Loaded,
        currentBalance: TotalFiatBalance,
        blockchainId: String,
    ): TotalFiatBalance {
        return with(currentBalance) {
            val yieldBalance = status.yieldBalance as? YieldBalance.Data
            val stakingBalance = yieldBalance?.getTotalWithRewardsStakingBalance(blockchainId).orZero()
            val fiatStakingBalance = status.fiatRate.times(stakingBalance)
            (this as? TotalFiatBalance.Loaded)?.copy(
                amount = this.amount + status.fiatAmount + fiatStakingBalance,
            ) ?: TotalFiatBalance.Loaded(
                amount = status.fiatAmount + fiatStakingBalance,
                source = status.sources.total,
            )
        }
    }

    private fun recalculateBalance(
        status: CryptoCurrencyStatus.Custom,
        currentBalance: TotalFiatBalance,
        blockchainId: String,
    ): TotalFiatBalance {
        return with(currentBalance) {
            val yieldBalance = (status.yieldBalance as? YieldBalance.Data)
                ?.getTotalWithRewardsStakingBalance(blockchainId).orZero()
            val fiatYieldBalance = status.fiatRate?.times(yieldBalance).orZero()

            (this as? TotalFiatBalance.Loaded)?.copy(
                amount = this.amount + status.fiatAmount.orZero() + fiatYieldBalance,
            ) ?: TotalFiatBalance.Loaded(
                amount = status.fiatAmount.orZero() + fiatYieldBalance,
                source = StatusSource.ACTUAL,
            )
        }
    }
}