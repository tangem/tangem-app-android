package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class SetConfirmationStateResetAssentTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState = prevState.confirmationState
        return prevState.copy(
            confirmationState = if (confirmationState is StakingStates.ConfirmationState.Data) {
                confirmationState.copy(
                    isPrimaryButtonEnabled = with(cryptoCurrencyStatus.value) {
                        sources.yieldBalanceSource.isActual() && sources.networkSource.isActual()
                    },
                    innerState = InnerConfirmationStakingState.ASSENT,
                )
            } else {
                confirmationState
            },
        )
    }
}