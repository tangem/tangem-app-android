package com.tangem.features.staking.impl.presentation.state.transformers.notifications

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class DismissStakingNotificationsStateTransformer(
    private val notification: Class<out NotificationUM>,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        val updatedNotifications = confirmationState?.notifications
            ?.filterNot { it::class == notification }?.toPersistentList()
            ?: persistentListOf()

        return prevState.copy(
            confirmationState = confirmationState?.copy(
                notifications = updatedNotifications,
            ) ?: StakingStates.ConfirmationState.Empty(),
        )
    }
}