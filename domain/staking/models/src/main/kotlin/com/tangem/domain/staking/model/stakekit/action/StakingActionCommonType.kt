package com.tangem.domain.staking.model.stakekit.action

sealed class StakingActionCommonType {

    data object Enter : StakingActionCommonType()
    data class Exit(val partiallyUnstakeDisabled: Boolean) : StakingActionCommonType()
    sealed class Pending : StakingActionCommonType() {
        data object Restake : Pending()
        data object Rewards : Pending()
        data object Other : Pending()
    }
}