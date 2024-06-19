package com.tangem.domain.staking.model.transaction

enum class StakingTransactionStatus {
    NOT_FOUND,
    CREATED,
    BLOCKED,
    WAITING_FOR_SIGNATURE,
    SIGNED,
    BROADCASTED,
    PENDING,
    CONFIRMED,
    FAILED,
    SKIPPED,
    UNKNOWN,
}