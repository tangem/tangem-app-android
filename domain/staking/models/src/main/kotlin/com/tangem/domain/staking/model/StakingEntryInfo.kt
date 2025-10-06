package com.tangem.domain.staking.model

import com.tangem.domain.staking.model.stakekit.Yield

data class StakingEntryInfo(
    val rewardInfo: Yield.RewardInfo,
    val rewardSchedule: Yield.Metadata.RewardSchedule,
    val tokenSymbol: String,
)