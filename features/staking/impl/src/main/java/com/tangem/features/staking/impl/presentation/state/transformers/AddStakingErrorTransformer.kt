package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class AddStakingErrorTransformer(
    private val error: StakingError,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState =
            prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState

        return prevState.copy(
            confirmationState = confirmationState.copy(
                notifications = (confirmationState.notifications + convertToNotification(error)).toPersistentList(),
                feeState = FeeState.Error,
            ),
        )
    }

    private fun convertToNotification(error: StakingError): StakingNotification {
        return when (error) {
            is StakingError.StakedPositionNotFoundError -> StakingNotification.Error.StakedPositionNotFoundError(
                message = error.toString(),
            )
// [REDACTED_TODO_COMMENT]
            else -> StakingNotification.Error.Common(
                subtitle = stringReference(error.toString()),
            )
        }
    }
}
