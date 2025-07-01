package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldSetMaxAmountTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer

internal class SwapAmountValueMaxTransformer(
    private val primaryMaximumAmountBoundary: EnterAmountBoundary,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary,
    private val primaryMinimumAmountBoundary: EnterAmountBoundary,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        return prevState
            .copy(selectedQuote = SwapQuoteUM.Loading)
            .updateAmount(
                onPrimaryAmount = {
                    copy(
                        amountField = AmountFieldSetMaxAmountTransformer(
                            cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                            maxAmount = primaryMaximumAmountBoundary,
                            minAmount = primaryMinimumAmountBoundary,
                        ).transform(prevState.primaryAmount.amountField),
                    )
                },
                onSecondaryAmount = {
                    copy(
                        amountField = AmountFieldSetMaxAmountTransformer(
                            cryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
                            maxAmount = secondaryMaximumAmountBoundary,
                            minAmount = secondaryMinimumAmountBoundary,
                        ).transform(prevState.secondaryAmount.amountField),
                    )
                },

            )
    }
}