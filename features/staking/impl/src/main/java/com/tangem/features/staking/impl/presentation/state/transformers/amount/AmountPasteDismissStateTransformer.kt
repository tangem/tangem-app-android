package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.AmountPastedTriggerDismissTransformer
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountPasteDismissStateTransformer : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            amountState = AmountPastedTriggerDismissTransformer().transform(prevState.amountState),
        )
    }
}