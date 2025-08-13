package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.AmountReduceToTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class SwapAmountReduceToTransformer(
    private val primaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val reduceToValue: BigDecimal,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        return prevState
            .copy(selectedQuote = SwapQuoteUM.Loading)
            .updateAmount(
                onPrimaryAmount = { primaryStatus ->
                    copy(
                        amountField = AmountReduceToTransformer(
                            cryptoCurrencyStatus = primaryStatus,
                            minimumTransactionAmount = primaryMinimumAmountBoundary,
                            value = reduceToValue,
                        ).transform(prevState.primaryAmount.amountField),
                    )
                },
                onSecondaryAmount = { secondaryStatus ->
                    copy(
                        amountField = AmountReduceToTransformer(
                            cryptoCurrencyStatus = secondaryStatus,
                            minimumTransactionAmount = secondaryMinimumAmountBoundary,
                            value = reduceToValue,
                        ).transform(prevState.secondaryAmount.amountField),
                    )
                },
            )
    }
}