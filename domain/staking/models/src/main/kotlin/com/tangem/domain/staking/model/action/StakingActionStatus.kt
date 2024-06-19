package com.tangem.domain.staking.model.action

enum class StakingActionStatus {
    CANCELED,
    CREATED,
    WAITING_FOR_NEXT,
    PROCESSING,
    FAILED,
    SUCCESS,
    UNKNOWN,
}