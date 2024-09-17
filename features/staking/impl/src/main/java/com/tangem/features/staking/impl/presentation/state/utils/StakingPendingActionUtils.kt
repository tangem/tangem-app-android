package com.tangem.features.staking.impl.presentation.state.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import kotlinx.collections.immutable.ImmutableList

@Suppress("CyclomaticComplexMethod")
internal fun StakingActionType?.getPendingActionTitle(): TextReference = when (this) {
    StakingActionType.CLAIM_REWARDS -> resourceReference(R.string.common_claim_rewards)
    StakingActionType.RESTAKE_REWARDS -> resourceReference(R.string.staking_restake_rewards)
    StakingActionType.WITHDRAW -> resourceReference(R.string.staking_withdraw)
    StakingActionType.RESTAKE -> resourceReference(R.string.staking_restake)
    StakingActionType.CLAIM_UNSTAKED -> resourceReference(R.string.staking_claim_unstaked)
    StakingActionType.UNLOCK_LOCKED -> resourceReference(R.string.staking_unlocked_locked)
    StakingActionType.STAKE_LOCKED -> resourceReference(R.string.staking_stake_locked)
    StakingActionType.VOTE -> resourceReference(R.string.staking_vote)
    StakingActionType.REVOKE -> resourceReference(R.string.staking_revoke)
    StakingActionType.VOTE_LOCKED -> resourceReference(R.string.staking_vote_locked)
    StakingActionType.REVOTE -> resourceReference(R.string.staking_revote)
    StakingActionType.REBOND -> resourceReference(R.string.staking_rebond)
    StakingActionType.MIGRATE -> resourceReference(R.string.staking_migrate)
    StakingActionType.STAKE -> resourceReference(R.string.common_stake)
    StakingActionType.UNSTAKE -> resourceReference(R.string.common_unstake)
    StakingActionType.UNKNOWN -> TextReference.EMPTY
    null -> TextReference.EMPTY
}

internal fun isSolanaWithdraw(networkId: String, pendingActions: ImmutableList<PendingAction>?): Boolean {
    val isSolana = isSolana(networkId)
    val isWithdraw = pendingActions?.all { it.type == StakingActionType.WITHDRAW } == true
    return isSolana && isWithdraw
}