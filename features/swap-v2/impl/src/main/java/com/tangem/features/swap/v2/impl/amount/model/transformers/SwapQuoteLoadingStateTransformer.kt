package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer

internal object SwapQuoteLoadingStateTransformer : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        when (prevState.selectedQuote) {
            is SwapQuoteUM.Allowance,
            is SwapQuoteUM.Content,
            -> return prevState // No need to set loading state if quotes are already display
            else -> Unit
        }
        return prevState.copy(
            selectedQuote = SwapQuoteUM.Loading,
            isPrimaryButtonEnabled = false,
        )
    }
}