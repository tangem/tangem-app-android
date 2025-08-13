package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer

internal class SwapAmountReduceByTransformer(
    private val primaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val reduceByData: AmountReduceByTransformer.ReduceByData,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        return prevState
            .copy(selectedQuote = SwapQuoteUM.Loading)
            .updateAmount(
                onPrimaryAmount = { primaryStatus ->
                    copy(
                        amountField = AmountReduceByTransformer(
                            cryptoCurrencyStatus = primaryStatus,
                            minimumTransactionAmount = primaryMinimumAmountBoundary,
                            value = reduceByData,
                        ).transform(prevState.primaryAmount.amountField),
                    )
                },
                onSecondaryAmount = { secondaryStatus ->
                    copy(
                        amountField = AmountReduceByTransformer(
                            cryptoCurrencyStatus = secondaryStatus,
                            minimumTransactionAmount = secondaryMinimumAmountBoundary,
                            value = reduceByData,
                        ).transform(prevState.secondaryAmount.amountField),
                    )
                },
            )
    }
}