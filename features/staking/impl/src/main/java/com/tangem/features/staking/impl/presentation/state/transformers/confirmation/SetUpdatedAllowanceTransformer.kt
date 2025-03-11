package com.tangem.features.staking.impl.presentation.state.transformers.confirmation

import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class SetUpdatedAllowanceTransformer(
    private val allowance: BigDecimal,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        return prevState.copy(
            confirmationState = confirmationState?.copy(allowance = allowance) ?: prevState.confirmationState,
        )
    }
}