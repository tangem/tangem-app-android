package com.tangem.domain.staking.model.stakekit.action

sealed class StakingActionCommonType {

    data object Enter : StakingActionCommonType()
    data object Exit : StakingActionCommonType()
    sealed class Pending : StakingActionCommonType() {
        data object Restake : Pending()
        data object Rewards : Pending()
        data object Other : Pending()
    }
}