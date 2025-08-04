package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountReduceByStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val minimumTransactionAmount: EnterAmountBoundary?,
    private val value: ReduceByData,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            amountState = AmountReduceByTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                minimumTransactionAmount = minimumTransactionAmount,
                value = value,
            ).transform(prevState.amountState),
        )
    }
}