package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer

internal object SwapQuoteLoadingStateTransformer : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        return prevState.copy(
            selectedQuote = if (prevState.selectedQuote is SwapQuoteUM.Empty) {
                SwapQuoteUM.Loading
            } else {
                prevState.selectedQuote
            },
        )
    }
}