package com.tangem.domain.models.staking

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Staking balance facade covering StakeKit and P2PEthPool balances
 */
@Serializable
sealed interface StakingBalance {

    val stakingId: StakingID
    val source: StatusSource

    val totalStaked: BigDecimal
    val totalRewards: BigDecimal?
    val unstakingAmount: BigDecimal?
    val withdrawableAmount: BigDecimal?

    @Serializable
    sealed interface Data : StakingBalance {

        /** Provider-agnostic list of balance entries for UI display */
        val entries: List<StakingBalanceEntry>

        @Serializable
        data class StakeKit(
            override val stakingId: StakingID,
            override val source: StatusSource,
            val balance: YieldBalanceItem,
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
        }

        @Serializable
        data class P2PEthPool(
            override val stakingId: StakingID,
            override val source: StatusSource,
            val accounts: List<P2PEthPoolStakingAccount>,
        ) : Data {

            override val totalStaked: SerializedBigDecimal = accounts.sumOf { it.stake.assets }

            override val totalRewards: SerializedBigDecimal = accounts.sumOf { it.stake.totalEarnedAssets }

            override val unstakingAmount: SerializedBigDecimal = accounts.sumOf { it.exitQueue.total }

            override val withdrawableAmount: SerializedBigDecimal = accounts.sumOf { it.availableToWithdraw }

            override val entries: List<StakingBalanceEntry> = accounts.flatMap { it.toStakingBalanceEntries() }
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
    }

    @Serializable
    data class Error(override val stakingId: StakingID) : StakingBalance {
        override val source: StatusSource get() = StatusSource.ACTUAL
        override val totalStaked: BigDecimal get() = BigDecimal.ZERO
        override val totalRewards: BigDecimal? get() = null
        override val unstakingAmount: BigDecimal? get() = null
        override val withdrawableAmount: BigDecimal? get() = null
    }

    fun copySealed(source: StatusSource): StakingBalance {
        return when (this) {
            is Data.StakeKit -> copy(source = source)
            is Data.P2PEthPool -> copy(source = source)
            is Empty -> copy(source = source)
            is Error -> this
        }
    }
}