package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal object SetApprovalInProgressTransformer : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val state = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        val notifications = state?.notifications?.toMutableList() ?: mutableListOf()

        notifications.add(
            StakingNotification.Warning.TransactionInProgress(
                title = resourceReference(R.string.warning_approval_in_progress_title),
                description = resourceReference(R.string.warning_approval_in_progress_message),
            ),
        )

        val updatedConfirmationState = state?.copy(
            notifications = notifications.toPersistentList(),
            isPrimaryButtonEnabled = false,
        ) ?: prevState.confirmationState

        return prevState.copy(
            confirmationState = updatedConfirmationState,
        )
    }
}