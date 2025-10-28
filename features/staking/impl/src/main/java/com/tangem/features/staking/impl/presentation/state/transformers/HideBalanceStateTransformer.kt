package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountBoundaryUpdateTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class HideBalanceStateTransformer(
    private val isBalanceHidden: Boolean,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val cryptoBalanceValue = cryptoCurrencyStatus.value
        val (amount, fiatAmount) = if (prevState.actionType !is StakingActionCommonType.Enter) {
            prevState.balanceState?.cryptoAmount to prevState.balanceState?.fiatAmount
        } else {
            cryptoBalanceValue.amount to cryptoBalanceValue.fiatAmount
        }
        val maxEnterAmount = EnterAmountBoundary(
            amount = amount,
            fiatAmount = fiatAmount,
            fiatRate = cryptoBalanceValue.fiatRate,
        )

        return prevState.copy(
            isBalanceHidden = isBalanceHidden,
            amountState = AmountBoundaryUpdateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                maxEnterAmount = maxEnterAmount,
                appCurrency = appCurrency,
                isBalanceHidden = isBalanceHidden,
            ).transform(prevState.amountState),
        )
    }
}