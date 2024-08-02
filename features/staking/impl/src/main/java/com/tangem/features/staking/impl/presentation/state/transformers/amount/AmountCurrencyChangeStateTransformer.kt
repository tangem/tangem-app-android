package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.AmountCurrencyTransformer
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountCurrencyChangeStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val value: Boolean,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            amountState = AmountCurrencyTransformer(cryptoCurrencyStatus, value).transform(prevState.amountState),
        )
    }
}