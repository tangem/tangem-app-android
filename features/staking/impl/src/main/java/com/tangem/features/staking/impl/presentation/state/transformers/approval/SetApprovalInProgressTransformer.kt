package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal object SetApprovalInProgressTransformer : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val state = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        val notifications = state?.notifications?.toMutableList() ?: mutableListOf()

        notifications.add(StakingNotification.Warning.TransactionInProgress.Approval)

        val updatedConfirmationState = state?.copy(
            notifications = notifications.toPersistentList(),
            isPrimaryButtonEnabled = false,
        ) ?: prevState.confirmationState

        return prevState.copy(
            confirmationState = updatedConfirmationState,
        )
    }
}