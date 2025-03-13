package com.tangem.domain.staking.toggles

interface StakingFeatureToggles {
    val isTonStakingEnabled: Boolean
    val isCardanoStakingEnabled: Boolean
}