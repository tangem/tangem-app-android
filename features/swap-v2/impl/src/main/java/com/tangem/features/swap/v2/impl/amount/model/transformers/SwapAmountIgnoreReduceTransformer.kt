package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.AmountIgnoreReduceTransformer
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer

internal object SwapAmountIgnoreReduceTransformer : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        return prevState
            .copy(selectedQuote = SwapQuoteUM.Loading)
            .updateAmount(
                onPrimaryAmount = { _ ->
                    copy(
                        amountField = AmountIgnoreReduceTransformer.transform(prevState.primaryAmount.amountField),
                    )
                },
                onSecondaryAmount = { _ ->
                    copy(
                        amountField = AmountIgnoreReduceTransformer.transform(prevState.secondaryAmount.amountField),
                    )
                },
            )
    }
}