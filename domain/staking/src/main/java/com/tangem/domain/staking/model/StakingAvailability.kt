package com.tangem.domain.staking.model

sealed class StakingAvailability {

    data class Available(val option: StakingOption) : StakingAvailability()

    data object Unavailable : StakingAvailability()

    data object TemporaryUnavailable : StakingAvailability()
}