package com.tangem.domain.staking.model.stakekit.action

sealed class StakingActionCommonType {

    data class Enter(val shouldSkipEnterAmount: Boolean) : StakingActionCommonType()
    data class Exit(val isPartiallyUnstakeDisabled: Boolean) : StakingActionCommonType()
    sealed class Pending : StakingActionCommonType() {
        data class Stake(val shouldSkipEnterAmount: Boolean) : Pending()
        data object Restake : Pending()
        data object Rewards : Pending()
        data object Other : Pending()
    }
}