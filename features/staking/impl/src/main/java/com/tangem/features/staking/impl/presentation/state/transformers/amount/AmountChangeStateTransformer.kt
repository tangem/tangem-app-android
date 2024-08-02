package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountChangeStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val value: String,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            amountState = AmountFieldChangeTransformer(cryptoCurrencyStatus, value).transform(prevState.amountState),
        )
    }
}
