package com.tangem.domain.staking.model

import com.tangem.domain.core.serialization.SerializedBigDecimal

data class StakingEntryInfo(
    val interestRate: SerializedBigDecimal,
    val periodInDays: Int,
    val tokenSymbol: String,
)
