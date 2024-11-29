package com.tangem.domain.staking.model.stakekit.action

enum class StakingActionType {
    STAKE,
    UNSTAKE,
    CLAIM_REWARDS,
    RESTAKE_REWARDS,
    WITHDRAW,
    RESTAKE,
    CLAIM_UNSTAKED,
    UNLOCK_LOCKED,
    STAKE_LOCKED,
    VOTE,
    REVOKE,
    VOTE_LOCKED,
    REVOTE,
    REBOND,
    MIGRATE,
    UNKNOWN,
    ;

    val asAnalyticName
        get() = when (this) {
            STAKE -> "Stake"
            UNSTAKE -> "Unstake"
            CLAIM_REWARDS -> "Claim Rewards"
            RESTAKE_REWARDS -> "Restake Rewards"
            CLAIM_UNSTAKED,
            WITHDRAW,
            -> "Withdraw"
            RESTAKE -> "Restake"
            UNLOCK_LOCKED -> "Unlock Locked"
            STAKE_LOCKED -> "Stake Locked"
            VOTE -> "Vote"
            REVOKE -> "Revoke"
            VOTE_LOCKED -> "Vote Locked"
            REVOTE -> "Revote"
            REBOND -> "Rebond"
            MIGRATE -> "Migrate"
            UNKNOWN -> "Unknown"
        }

    val canChooseValidator
        get() = when (this) {
            STAKE,
            RESTAKE,
            VOTE,
            VOTE_LOCKED,
            -> true

            UNSTAKE,
            CLAIM_REWARDS,
            RESTAKE_REWARDS,
            CLAIM_UNSTAKED,
            WITHDRAW,
            UNLOCK_LOCKED,
            STAKE_LOCKED,
            REVOKE,
            REVOTE,
            REBOND,
            MIGRATE,
            UNKNOWN,
            -> false
        }
}