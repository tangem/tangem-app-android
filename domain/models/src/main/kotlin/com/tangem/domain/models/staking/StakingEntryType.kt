package com.tangem.domain.models.staking

import kotlinx.serialization.Serializable

@Serializable
enum class StakingEntryType {
    AVAILABLE,
    STAKED,
    PREPARING,
    LOCKED,
    UNSTAKING,
    UNLOCKING,
    WITHDRAWABLE,
    REWARDS,
    UNKNOWN,
    ;

    companion object {
        fun fromBalanceType(type: BalanceType): StakingEntryType = when (type) {
            BalanceType.AVAILABLE -> AVAILABLE
            BalanceType.STAKED -> STAKED
            BalanceType.PREPARING -> PREPARING
            BalanceType.LOCKED -> LOCKED
            BalanceType.UNSTAKING -> UNSTAKING
            BalanceType.UNLOCKING -> UNLOCKING
            BalanceType.UNSTAKED -> WITHDRAWABLE // stakekit's UNSTAKED = ready to withdraw
            BalanceType.REWARDS -> REWARDS
            BalanceType.UNKNOWN -> UNKNOWN
        }
    }
}