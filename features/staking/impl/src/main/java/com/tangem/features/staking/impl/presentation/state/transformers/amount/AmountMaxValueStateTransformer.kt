package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.MaxEnterAmountConverter
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldSetMaxAmountTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountMaxValueStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val minimumTransactionAmount: EnterAmountBoundary?,
    private val actionType: StakingActionCommonType,
    private val yield: Yield,
) : Transformer<StakingUiState> {

    private val maxEnterAmountConverter = MaxEnterAmountConverter()

    override fun transform(prevState: StakingUiState): StakingUiState {
        val maxEnterAmount = if (actionType is StakingActionCommonType.Exit) {
            EnterAmountBoundary(
                amount = prevState.balanceState?.cryptoAmount,
                fiatAmount = prevState.balanceState?.fiatAmount,
                fiatRate = cryptoCurrencyStatus.value.fiatRate,
            )
        } else {
            maxEnterAmountConverter.convert(cryptoCurrencyStatus)
        }

        val updatedAmountState = AmountFieldSetMaxAmountTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxAmount = maxEnterAmount,
            minAmount = minimumTransactionAmount,
        ).transform(prevState.amountState)
        return prevState.copy(
            amountState = AmountRequirementStateTransformer(
                maxAmount = maxEnterAmount,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                yield = yield,
                actionType = prevState.actionType,
            ).transform(updatedAmountState),
        )
    }
}