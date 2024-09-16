package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldMaxAmountTransformer
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountMaxValueStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val yield: Yield,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val updatedAmountState = AmountFieldMaxAmountTransformer(cryptoCurrencyStatus).transform(prevState.amountState)
        return prevState.copy(
            amountState = AmountRequirementStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                yield = yield,
                actionType = prevState.actionType,
            ).transform(updatedAmountState),
        )
    }
}