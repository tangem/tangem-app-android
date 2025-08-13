package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceToTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class AmountReduceToStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val minimumTransactionAmount: EnterAmountBoundary?,
    private val value: BigDecimal,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            amountState = AmountReduceToTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                minimumTransactionAmount = minimumTransactionAmount,
                value = value,
            ).transform(prevState.amountState),
        )
    }
}