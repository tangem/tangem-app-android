package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer

internal class SwapAmountValueChangeTransformer(
    private val primaryMaximumAmountBoundary: EnterAmountBoundary,
    private val primaryMinimumAmountBoundary: EnterAmountBoundary,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val value: String,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val updatedState = prevState.updateAmount(
            onPrimaryAmount = { primaryStatus ->
                copy(
                    amountField = AmountFieldChangeTransformer(
                        cryptoCurrencyStatus = primaryStatus,
                        maxEnterAmount = primaryMaximumAmountBoundary,
                        minimumTransactionAmount = primaryMinimumAmountBoundary,
                        value = value,
                    ).transform(prevState.primaryAmount.amountField),
                )
            },
            onSecondaryAmount = { secondaryStatus ->
                if (secondaryMaximumAmountBoundary != null) {
                    copy(
                        amountField = AmountFieldChangeTransformer(
                            cryptoCurrencyStatus = secondaryStatus,
                            maxEnterAmount = secondaryMaximumAmountBoundary,
                            minimumTransactionAmount = secondaryMinimumAmountBoundary,
                            value = value,
                        ).transform(prevState.secondaryAmount.amountField),
                    )
                } else {
                    this
                }
            },
        )

        return (updatedState as? SwapAmountUM.Content)?.copy(
            selectedQuote = if (updatedState.isPrimaryButtonEnabled) {
                SwapQuoteUM.Empty
            } else {
                SwapQuoteUM.Loading
            },
        ) ?: updatedState
    }
}