package com.tangem.domain.staking.model.common

import kotlinx.serialization.Serializable

@Serializable
enum class RewardSchedule {
    BLOCK,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    ERA,
    EPOCH,
    UNKNOWN,
}