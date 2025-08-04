package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.AmountCurrencyTransformer
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.utils.transformer.Transformer

internal class SwapAmountChangeCurrencyTransformer(
    private val isFiatSelected: Boolean,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState
        return prevState.updateAmount(
            onPrimaryAmount = { primaryStatus ->
                copy(
                    amountField = AmountCurrencyTransformer(
                        cryptoCurrencyStatus = primaryStatus,
                        value = isFiatSelected,
                    ).transform(prevState.primaryAmount.amountField),
                )
            },
            onSecondaryAmount = { secondaryStatus ->
                copy(
                    amountField = AmountCurrencyTransformer(
                        cryptoCurrencyStatus = secondaryStatus,
                        value = isFiatSelected,
                    ).transform(prevState.secondaryAmount.amountField),
                )
            },
        )
    }
}