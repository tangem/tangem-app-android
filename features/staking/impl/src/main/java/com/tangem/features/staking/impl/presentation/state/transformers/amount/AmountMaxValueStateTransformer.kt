package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldMaxAmountTransformer
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountMaxValueStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            amountState = AmountFieldMaxAmountTransformer(cryptoCurrencyStatus).transform(prevState.amountState),
        )
    }
}
