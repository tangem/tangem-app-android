package com.tangem.domain.staking.model

import com.tangem.domain.staking.model.stakekit.Yield
import java.math.BigDecimal

data class StakingEntryInfo(
    val apr: BigDecimal,
    val rewardSchedule: Yield.Metadata.RewardSchedule,
    val tokenSymbol: String,
)