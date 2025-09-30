package com.tangem.domain.tokens.operations

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.staking.utils.getTotalWithRewardsStakingBalance
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

/**
 * Utility to calculate total fiat balance from a set of [CryptoCurrencyStatus].
 *
 * The calculation considers various states of each cryptocurrency, including loading,
 * unreachable, no amount, no account, loaded, and custom states.
 *
 * The result is a [TotalFiatBalance], which can be in one of the following states:
 * - [TotalFiatBalance.Loading]: If any cryptocurrency is still loading.
 * - [TotalFiatBalance.Failed]: If any cryptocurrency is in a non-computable state (e.g., no quote).
 * - [TotalFiatBalance.Loaded]: If all cryptocurrencies are computable, containing the total fiat amount,
 *   a flag indicating if all amounts were summarized, and the source of the data.
 *
 * The calculation also takes into account staking balances when available.
 */
object TotalFiatBalanceCalculator {

    fun calculate(statuses: NonEmptyList<CryptoCurrencyStatus>): TotalFiatBalance {
        val computationState = ComputationState.resolve(statuses)

        return when (computationState) {
            ComputationState.LOADING -> TotalFiatBalance.Loading
            ComputationState.NON_COMPUTABLE -> TotalFiatBalance.Failed
            ComputationState.COMPUTABLE -> compute(statuses)
        }
    }

    fun calculate(balances: List<TotalFiatBalance>): TotalFiatBalance {
        val nonEmptyBalances = balances.toNonEmptyListOrNull()
            ?: return TokenList.Empty.totalFiatBalance

        val computationState = ComputationState.resolve(balances = nonEmptyBalances)

        return when (computationState) {
            ComputationState.LOADING -> TotalFiatBalance.Loading
            ComputationState.NON_COMPUTABLE -> TotalFiatBalance.Failed
            ComputationState.COMPUTABLE -> {
                val loaded = balances.filterIsInstance<TotalFiatBalance.Loaded>()

                TotalFiatBalance.Loaded(
                    amount = loaded.sumOf(TotalFiatBalance.Loaded::amount),
                    source = loaded.map(TotalFiatBalance.Loaded::source).getResultStatusSource(),
                )
            }
        }
    }

    private fun compute(statuses: NonEmptyList<CryptoCurrencyStatus>): TotalFiatBalance {
        var mutableBalance: TotalFiatBalance = TotalFiatBalance.Loading

        for (token in statuses) {
            val blockchainId = token.currency.network.rawId

            when (val status = token.value) {
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                is CryptoCurrencyStatus.NoAccount,
                -> {
                    mutableBalance = mutableBalance.plusEmptyBalance(status)
                }
                is CryptoCurrencyStatus.Loaded -> {
                    mutableBalance = mutableBalance.plusLoaded(status, blockchainId)
                }
                is CryptoCurrencyStatus.Custom -> {
                    mutableBalance = mutableBalance.plusLoaded(status, blockchainId)
                }
                // Non computable states, should be handled before
                CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.NoQuote,
                -> continue
            }
        }

        return mutableBalance.updateSource(statuses)
    }

    private fun TotalFiatBalance.plusEmptyBalance(status: CryptoCurrencyStatus.Value): TotalFiatBalance {
        return fold(
            ifLoaded = { it },
            ifNot = {
                TotalFiatBalance.Loaded(
                    amount = BigDecimal.ZERO,
                    source = status.sources.total, // never mind
                )
            },
        )
    }

    private fun TotalFiatBalance.plusLoaded(
        status: CryptoCurrencyStatus.Loaded,
        blockchainId: String,
    ): TotalFiatBalance {
        val fiatStakingBalance = status.getFiatStakingBalance(blockchainId)

        return fold(
            ifLoaded = { loaded ->
                loaded.copy(amount = loaded.amount + status.fiatAmount + fiatStakingBalance)
            },
            ifNot = {
                TotalFiatBalance.Loaded(
                    amount = status.fiatAmount + fiatStakingBalance,
                    source = status.sources.total, // never mind
                )
            },
        )
    }

    private fun TotalFiatBalance.plusLoaded(
        status: CryptoCurrencyStatus.Custom,
        blockchainId: String,
    ): TotalFiatBalance {
        val fiatStakingBalance = status.getFiatStakingBalance(blockchainId)

        return fold(
            ifLoaded = { loaded ->
                loaded.copy(
                    amount = loaded.amount + status.fiatAmount.orZero() + fiatStakingBalance,
                )
            },
            ifNot = {
                TotalFiatBalance.Loaded(
                    amount = status.fiatAmount.orZero() + fiatStakingBalance,
                    source = StatusSource.ACTUAL,
                )
            },
        )
    }

    private fun TotalFiatBalance.updateSource(statuses: NonEmptyList<CryptoCurrencyStatus>): TotalFiatBalance {
        return fold(
            ifLoaded = { loaded ->
                loaded.copy(
                    source = statuses.map { it.value.sources.total }.getResultStatusSource(),
                )
            },
            ifNot = { this },
        )
    }

    private fun CryptoCurrencyStatus.Loaded.getFiatStakingBalance(blockchainId: String): BigDecimal {
        val yieldBalance = yieldBalance as? YieldBalance.Data
        val stakingBalance = yieldBalance?.getTotalWithRewardsStakingBalance(blockchainId).orZero()

        return fiatRate.times(stakingBalance)
    }

    private fun CryptoCurrencyStatus.Custom.getFiatStakingBalance(blockchainId: String): BigDecimal {
        val yieldBalance = yieldBalance as? YieldBalance.Data
        val stakingBalance = yieldBalance?.getTotalWithRewardsStakingBalance(blockchainId).orZero()

        return fiatRate?.times(stakingBalance).orZero()
    }

    private inline fun TotalFiatBalance.fold(
        ifLoaded: (TotalFiatBalance.Loaded) -> TotalFiatBalance,
        ifNot: () -> TotalFiatBalance,
    ): TotalFiatBalance {
        return if (this is TotalFiatBalance.Loaded) {
            ifLoaded(this)
        } else {
            ifNot()
        }
    }

    private enum class ComputationState {

        LOADING, NON_COMPUTABLE, COMPUTABLE;

        companion object {

            fun resolve(statuses: NonEmptyList<CryptoCurrencyStatus>): ComputationState {
                for (status in statuses) {
                    when (status.value) {
                        CryptoCurrencyStatus.Loading -> {
                            return LOADING
                        }
                        is CryptoCurrencyStatus.NoQuote,
                        is CryptoCurrencyStatus.MissedDerivation,
                        -> {
                            return NON_COMPUTABLE
                        }
                        is CryptoCurrencyStatus.Unreachable,
                        is CryptoCurrencyStatus.NoAmount,
                        -> {
                            val blockchainId = status.currency.network.rawId
                            if (!BlockchainUtils.isIncludeToBalanceOnError(blockchainId)) {
                                return NON_COMPUTABLE
                            }
                        }
                        is CryptoCurrencyStatus.Loaded,
                        is CryptoCurrencyStatus.Custom,
                        is CryptoCurrencyStatus.NoAccount,
                        -> continue
                    }
                }

                return COMPUTABLE
            }

            fun resolve(balances: List<TotalFiatBalance>): ComputationState {
                for (balance in balances) {
                    when (balance) {
                        TotalFiatBalance.Failed -> return NON_COMPUTABLE
                        TotalFiatBalance.Loading -> return LOADING
                        is TotalFiatBalance.Loaded -> continue
                    }
                }

                return COMPUTABLE
            }
        }
    }
}