package com.tangem.domain.staking.model.common

import kotlinx.serialization.Serializable

@Serializable
enum class RewardType {
    APY, // compound rate
    APR, // simple rate
    UNKNOWN,
}