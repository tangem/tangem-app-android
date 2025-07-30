package com.tangem.domain.staking.model

import kotlinx.serialization.Serializable

@Serializable
data class StakingID(val integrationId: String, val address: String)