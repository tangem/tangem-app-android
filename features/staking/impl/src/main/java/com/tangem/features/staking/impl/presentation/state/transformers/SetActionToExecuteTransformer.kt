package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

internal class SetActionToExecuteTransformer(
    private val actionTypeToOverwrite: StakingActionCommonType,
    private val pendingAction: PendingAction?,
    private val pendingActions: ImmutableList<PendingAction>?,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        return prevState.copy(
            actionType = actionTypeToOverwrite,
            confirmationState = confirmationState?.copy(
                pendingAction = pendingAction,
                pendingActions = pendingActions,
            ) ?: StakingStates.ConfirmationState.Empty(),
        )
    }
}