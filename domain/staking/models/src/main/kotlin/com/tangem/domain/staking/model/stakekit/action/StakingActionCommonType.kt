package com.tangem.domain.staking.model.stakekit.action

sealed class StakingActionCommonType {

    data class Enter(val skipEnterAmount: Boolean) : StakingActionCommonType()
    data class Exit(val partiallyUnstakeDisabled: Boolean) : StakingActionCommonType()
    sealed class Pending : StakingActionCommonType() {
        data class Stake(val skipEnterAmount: Boolean) : Pending()
        data object Restake : Pending()
        data object Rewards : Pending()
        data object Other : Pending()
    }
}