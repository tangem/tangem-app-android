package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.AmountPastedTriggerDismissTransformer
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.utils.transformer.Transformer

internal object SwapAmountPasteTransformer : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        return prevState.updateAmount(
            onPrimaryAmount = {
                copy(
                    amountField = AmountPastedTriggerDismissTransformer.transform(
                        prevState = prevState.primaryAmount.amountField,
                    ),
                )
            },
            onSecondaryAmount = {
                copy(
                    amountField = AmountPastedTriggerDismissTransformer.transform(
                        prevState = prevState.secondaryAmount.amountField,
                    ),
                )
            },
        )
    }
}