package com.tangem.domain.staking.model

import com.tangem.domain.staking.model.stakekit.Yield

sealed class StakingAvailability {

    data class Available(val yield: Yield) : StakingAvailability()

    data object Unavailable : StakingAvailability()

    data object TemporaryUnavailable : StakingAvailability()
}