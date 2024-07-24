package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldMaxAmountTransformer
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountMaxValueStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val yield: Yield,
) : Transformer<StakingUiState> {

    private val amountRequirementStateTransformer by lazy(LazyThreadSafetyMode.NONE) {
        val value = cryptoCurrencyStatus.value.amount
            ?.parseBigDecimal(cryptoCurrencyStatus.currency.decimals)
            .orEmpty()
        AmountRequirementStateTransformer(
            cryptoCurrencyStatus,
            yield,
            value,
        )
    }

    override fun transform(prevState: StakingUiState): StakingUiState {
        val updatedAmountState = AmountFieldMaxAmountTransformer(cryptoCurrencyStatus).transform(prevState.amountState)
        return prevState.copy(
            amountState = amountRequirementStateTransformer.transform(updatedAmountState),
        )
    }
}
