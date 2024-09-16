package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SetConfirmationStateCompletedTransformer(
    private val txUrl: String,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            confirmationState = prevState.confirmationState.copyWrapped(),
        )
    }

    private fun StakingStates.ConfirmationState.copyWrapped(): StakingStates.ConfirmationState {
        if (this is StakingStates.ConfirmationState.Data) {
            return copy(
                isPrimaryButtonEnabled = true,
                innerState = InnerConfirmationStakingState.COMPLETED,
                validatorState = validatorState.copySealed(isClickable = false),
                footerText = TextReference.EMPTY,
                notifications = persistentListOf(),
                transactionDoneState = TransactionDoneState.Content(
                    timestamp = System.currentTimeMillis(),
                    txUrl = txUrl,
                ),
            )
        } else {
            return this
        }
    }
}
