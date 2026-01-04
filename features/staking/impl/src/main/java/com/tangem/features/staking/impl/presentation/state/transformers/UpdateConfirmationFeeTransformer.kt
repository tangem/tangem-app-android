package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class UpdateConfirmationFeeTransformer(
    private val newFee: Fee,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
            ?: return prevState

        val currentFeeState = confirmationState.feeState as? FeeState.Content
            ?: return prevState

        val updatedFeeState = currentFeeState.copy(fee = newFee)

        return prevState.copy(
            confirmationState = confirmationState.copy(
                feeState = updatedFeeState,
            ),
        )
    }
}