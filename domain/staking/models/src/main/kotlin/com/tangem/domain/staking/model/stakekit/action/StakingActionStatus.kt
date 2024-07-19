package com.tangem.domain.staking.model.stakekit.action

enum class StakingActionStatus {
    CANCELED,
    CREATED,
    WAITING_FOR_NEXT,
    PROCESSING,
    FAILED,
    SUCCESS,
    UNKNOWN,
}