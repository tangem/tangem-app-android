package com.tangem.domain.models.staking

import kotlinx.serialization.Serializable

enum class StakingProvider {
    STAKEKIT,
    P2P,
}

@Serializable
data class StakingID(
    val integrationId: String,
    val address: String,
)