package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountChangeStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val yield: Yield,
    private val value: String,
) : Transformer<StakingUiState> {

    private val amountRequirementStateTransformer = AmountRequirementStateTransformer(
        cryptoCurrencyStatus,
        yield,
        value,
    )

    override fun transform(prevState: StakingUiState): StakingUiState {
        val updatedAmountState = AmountFieldChangeTransformer(
            cryptoCurrencyStatus,
            value,
        ).transform(prevState.amountState)

        return prevState.copy(
            amountState = amountRequirementStateTransformer.transform(updatedAmountState),
        )
    }
}