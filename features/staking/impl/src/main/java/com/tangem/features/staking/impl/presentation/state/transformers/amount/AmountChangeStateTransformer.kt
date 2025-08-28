package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.converters.MaxEnterAmountConverter
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class AmountChangeStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val minimumTransactionAmount: EnterAmountBoundary?,
    private val value: String,
    private val yield: Yield,
) : Transformer<StakingUiState> {

    private val maxEnterAmountConverter = MaxEnterAmountConverter()

    override fun transform(prevState: StakingUiState): StakingUiState {
        val actionType = prevState.actionType
        val maxEnterAmount = if (actionType is StakingActionCommonType.Exit) {
            EnterAmountBoundary(
                amount = prevState.balanceState?.cryptoAmount,
                fiatAmount = prevState.balanceState?.fiatAmount,
                fiatRate = cryptoCurrencyStatus.value.fiatRate,
            )
        } else {
            maxEnterAmountConverter.convert(cryptoCurrencyStatus)
        }

        val updatedAmountState = AmountFieldChangeTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxEnterAmount = maxEnterAmount,
            minimumTransactionAmount = minimumTransactionAmount,
            value = value,
        ).transform(prevState.amountState)

        return prevState.copy(
            amountState = AmountRequirementStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                maxAmount = maxEnterAmount,
                yield = yield,
                actionType = prevState.actionType,
            ).transform(updatedAmountState),
        )
    }
}