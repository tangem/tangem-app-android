package com.tangem.domain.staking.model.common

import kotlinx.serialization.Serializable

@Serializable
enum class RewardClaiming {
    AUTO,
    MANUAL,
    UNKNOWN,
}