package com.tangem.domain.models.staking

import kotlinx.serialization.Serializable

@Serializable
data class StakingID(val integrationId: String, val address: String)