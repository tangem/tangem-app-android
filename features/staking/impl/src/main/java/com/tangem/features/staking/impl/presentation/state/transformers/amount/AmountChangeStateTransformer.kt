package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.MaxEnterAmountConverter
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountChangeStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val value: String,
    private val yield: Yield,
) : Transformer<StakingUiState> {

    private val maxEnterAmountConverter = MaxEnterAmountConverter()

    override fun transform(prevState: StakingUiState): StakingUiState {
        val maxEnterAmount = maxEnterAmountConverter.convert(
            cryptoCurrencyStatus,
        )
        val updatedAmountState = AmountFieldChangeTransformer(maxEnterAmount, value).transform(prevState.amountState)

        return prevState.copy(
            amountState = AmountRequirementStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                yield = yield,
                actionType = prevState.actionType,
            ).transform(updatedAmountState),
        )
    }
}