package com.tangem.domain.staking.model

import java.math.BigDecimal

data class StakingEntryInfo(
    val percent: BigDecimal,
    val periodInDays: Int,
    val tokenSymbol: String,
)
