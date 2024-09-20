package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.transformer.Transformer

internal class SetBalanceStateTransformer(
    private val balanceState: BalanceState? = null,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val possibleConfirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
            ?: return prevState

        return prevState.copy(
            confirmationState = possibleConfirmationState.copy(balanceState = balanceState),
        )
    }
}