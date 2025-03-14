package com.tangem.features.staking.impl.presentation.state.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.lib.crypto.BlockchainUtils.isBSC
import com.tangem.lib.crypto.BlockchainUtils.isCardano
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.lib.crypto.BlockchainUtils.isTron
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Suppress("CyclomaticComplexMethod")
internal fun StakingActionType?.getPendingActionTitle(): TextReference = when (this) {
    StakingActionType.CLAIM_REWARDS -> resourceReference(R.string.common_claim_rewards)
    StakingActionType.RESTAKE_REWARDS -> resourceReference(R.string.staking_restake_rewards)
    StakingActionType.CLAIM_UNSTAKED,
    StakingActionType.WITHDRAW,
    -> resourceReference(R.string.staking_withdraw)
    StakingActionType.RESTAKE -> resourceReference(R.string.staking_restake)
    StakingActionType.UNLOCK_LOCKED -> resourceReference(R.string.staking_unlocked_locked)
    StakingActionType.STAKE_LOCKED -> resourceReference(R.string.staking_stake_locked)
    StakingActionType.VOTE -> resourceReference(R.string.staking_vote)
    StakingActionType.REVOKE -> resourceReference(R.string.staking_revoke)
    StakingActionType.VOTE_LOCKED -> resourceReference(R.string.staking_vote_locked)
    StakingActionType.REVOTE -> resourceReference(R.string.staking_revote)
    StakingActionType.REBOND -> resourceReference(R.string.staking_rebond)
    StakingActionType.MIGRATE -> resourceReference(R.string.staking_migrate)
    StakingActionType.STAKE -> resourceReference(R.string.staking_restake)
    StakingActionType.UNSTAKE -> resourceReference(R.string.common_unstake)
    StakingActionType.UNKNOWN -> TextReference.EMPTY
    null -> TextReference.EMPTY
}

internal fun isSingleAction(networkId: String, activeStake: BalanceState): Boolean {
    val isSingleAction = activeStake.pendingActions.size <= 1 // Either single or none pending actions
    val isCompositePendingActions = isCompositePendingActions(networkId, activeStake.pendingActions)
    val isRestake = activeStake.pendingActions.any { it.type.isRestake }

    return isSingleAction && !isRestake || isCompositePendingActions
}

internal fun withStubUnstakeAction(networkId: String, activeStake: BalanceState) = if (isStubUnstakeAction(networkId)) {
    activeStake.pendingActions.plus(
        PendingAction(
            type = StakingActionType.UNSTAKE,
            passthrough = "",
            args = null,
        ),
    ).toPersistentList()
} else {
    activeStake.pendingActions
}

internal fun isTronStakedBalance(networkId: String, pendingAction: PendingAction?): Boolean {
    return isTron(networkId) && pendingAction?.type == StakingActionType.REVOTE
}

internal fun isCompositePendingActions(networkId: String, pendingActions: ImmutableList<PendingAction>?): Boolean {
    return when {
        isSolana(networkId) -> pendingActions?.any { it.type == StakingActionType.WITHDRAW } == true
        else -> false
    }
}

private fun isStubUnstakeAction(networkId: String): Boolean {
    return isBSC(networkId) || isCardano(networkId)
}