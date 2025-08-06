package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.utils.transformer.Transformer

internal class SwapAmountUpdateBalanceTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val primaryMaximumAmountBoundary: EnterAmountBoundary,
    private val primaryMinimumAmountBoundary: EnterAmountBoundary,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        val state = prevState as? SwapAmountUM.Content ?: return prevState
        val primaryAmountUM = state.primaryAmount as? SwapAmountFieldUM.Content ?: return prevState
        val amountField = primaryAmountUM.amountField as? AmountState.Data ?: return prevState
        val amountValue = if (amountField.amountTextField.isFiatValue) {
            amountField.amountTextField.fiatValue
        } else {
            amountField.amountTextField.value
        }
        return state.copy(
            primaryCryptoCurrencyStatus = cryptoCurrencyStatus,
            primaryAmount = primaryAmountUM.copy(
                amountField = AmountFieldChangeTransformer(
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    maxEnterAmount = primaryMaximumAmountBoundary,
                    minimumTransactionAmount = primaryMinimumAmountBoundary,
                    value = amountValue,
                ).transform(prevState.primaryAmount.amountField),
            ),
        )
    }
}