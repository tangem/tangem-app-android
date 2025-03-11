package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class AddStakingErrorTransformer(
    private val error: StakingError? = null,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState =
            prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState

        val notifications = buildList {
            addAll(confirmationState.notifications)
            error?.let { add(convertToNotification(it)) }
        }.toPersistentList()

        return prevState.copy(
            confirmationState = confirmationState.copy(
                notifications = notifications,
                feeState = FeeState.Error,
            ),
        )
    }

    private fun convertToNotification(error: StakingError): NotificationUM {
        return StakingNotification.Error.Common(
            subtitle = stringReference(error.toString()),
        )
    }
}