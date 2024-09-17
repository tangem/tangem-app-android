package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountReduceByStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val value: ReduceByData,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            amountState = AmountReduceByTransformer(cryptoCurrencyStatus, value).transform(prevState.amountState),
        )
    }
}