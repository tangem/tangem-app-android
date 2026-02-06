package com.tangem.domain.staking.model.common

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class RewardInfo(
    val rate: SerializedBigDecimal,
    val type: RewardType,
)