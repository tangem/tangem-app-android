package com.tangem.domain.staking.model

sealed class StakingApproval {

    data class Needed(val spenderAddress: String) : StakingApproval()

    data object Empty : StakingApproval()
}