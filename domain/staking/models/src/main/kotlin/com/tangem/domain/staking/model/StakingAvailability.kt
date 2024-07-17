package com.tangem.domain.staking.model

sealed class StakingAvailability {

    data class Available(val integrationId: String) : StakingAvailability()

    data object Unavailable : StakingAvailability()

    data object TemporaryDisabled : StakingAvailability()
}
