package com.tangem.domain.models.staking

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed interface StakingEntryActions {

    @Serializable
    data class StakeKit(
        val pendingActions: List<PendingAction>,
        val pendingActionsConstraints: List<PendingActionConstraints>,
    ) : StakingEntryActions {
        val hasPendingActions: Boolean get() = pendingActions.isNotEmpty()
    }

    @Serializable
    data class P2PEthPool(
        val ticket: String?,
        val estimatedWithdrawalDate: Instant?,
        val isClaimable: Boolean,
    ) : StakingEntryActions
}