package com.tangem.domain.models.staking

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Staking balance facade covering StakeKit and P2PEthPool balances
 */
@Serializable
sealed interface StakingBalance : BalanceContribution {

    override val kind: String get() = CONTRIBUTION_KIND
    val stakingId: StakingID
    override val source: StatusSource

    val totalStaked: BigDecimal
    val totalRewards: BigDecimal?
    val unstakingAmount: BigDecimal?
    val withdrawableAmount: BigDecimal?

    /**
     * A non-empty staking balance. Contributes to the owning currency's total, so it is also a
     * [BalanceContribution] — see [totalDeltaCryptoAmount].
     */
    @Serializable
    sealed interface Data : StakingBalance {

        /** Provider-agnostic list of balance entries for UI display */
        val entries: List<StakingBalanceEntry>

        /**
         * `true` when the staked principal is **already part of the network balance** (Cardano), so only rewards
         * may be added on top of it. `false` — the default and the case for every other network — means the whole
         * staking balance sits outside the network amount.
         *
         * Resolved from the owning currency's network by `CryptoCurrencyStatusFactory`, which stamps it when it
         * attaches the balance to a status. It is deliberately not derived here: [stakingId] carries no network,
         * and the decision must match the network of the currency the balance is folded into.
         */
        val isStakedIncludedInNetworkBalance: Boolean

        @Serializable
        data class StakeKit(
            override val stakingId: StakingID,
            override val source: StatusSource,
            val balance: YieldBalanceItem,
            override val isStakedIncludedInNetworkBalance: Boolean = false,
        ) : Data {

            override val totalStaked: SerializedBigDecimal = balance.items
                .filter { it.type == BalanceType.STAKED }
                .sumOf { it.amount }

            override val totalRewards: SerializedBigDecimal = balance.items
                .filter { it.type == BalanceType.REWARDS }
                .sumOf { it.amount }

            override val unstakingAmount: SerializedBigDecimal = balance.items
                .filter { it.type == BalanceType.UNSTAKING || it.type == BalanceType.UNLOCKING }
                .sumOf { it.amount }

            override val withdrawableAmount: SerializedBigDecimal = balance.items
                .filter { it.type == BalanceType.UNSTAKED }
                .sumOf { it.amount }

            override val entries: List<StakingBalanceEntry> = balance.items.toStakingBalanceEntries()

            /** Every balance item counts — not only the [totalStaked] / [unstakingAmount] / [totalRewards] buckets. */
            override fun totalDeltaCryptoAmount(): BigDecimal {
                return if (isStakedIncludedInNetworkBalance) {
                    totalRewards
                } else {
                    balance.items.sumOf { it.amount }
                }
            }
        }

        @Serializable
        data class P2PEthPool(
            override val stakingId: StakingID,
            override val source: StatusSource,
            val accounts: List<P2PEthPoolStakingAccount>,
            override val isStakedIncludedInNetworkBalance: Boolean = false,
        ) : Data {

            override val totalStaked: SerializedBigDecimal = accounts.sumOf { it.stake.assets }

            override val totalRewards: SerializedBigDecimal = accounts.sumOf { it.stake.totalEarnedAssets }

            override val unstakingAmount: SerializedBigDecimal = accounts.sumOf { it.unstakingAssets }

            override val withdrawableAmount: SerializedBigDecimal = accounts.sumOf { it.withdrawableAssets }

            override val entries: List<StakingBalanceEntry> = accounts.flatMap { it.toStakingBalanceEntries() }

            override fun totalDeltaCryptoAmount(): BigDecimal {
                return if (isStakedIncludedInNetworkBalance) {
                    totalRewards
                } else {
                    totalStaked + unstakingAmount + withdrawableAmount + totalRewards
                }
            }
        }
    }

    @Serializable
    data class Empty(
        override val stakingId: StakingID,
        override val source: StatusSource,
    ) : StakingBalance {
        override val totalStaked: BigDecimal get() = BigDecimal.ZERO
        override val totalRewards: BigDecimal? get() = null
        override val unstakingAmount: BigDecimal? get() = null
        override val withdrawableAmount: BigDecimal? get() = null
        override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal.ZERO
    }

    @Serializable
    data class Error(override val stakingId: StakingID) : StakingBalance {
        override val source: StatusSource get() = StatusSource.ACTUAL
        override val totalStaked: BigDecimal get() = BigDecimal.ZERO
        override val totalRewards: BigDecimal? get() = null
        override val unstakingAmount: BigDecimal? get() = null
        override val withdrawableAmount: BigDecimal? get() = null
        override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal.ZERO
    }

    fun copySealed(source: StatusSource): StakingBalance {
        return when (this) {
            is Data.StakeKit -> copy(source = source)
            is Data.P2PEthPool -> copy(source = source)
            is Empty -> copy(source = source)
            is Error -> this
        }
    }

    companion object {

        /** [BalanceContribution.kind] of every staking balance. Owned here, not by the base contract. */
        const val CONTRIBUTION_KIND = "staking"
    }
}