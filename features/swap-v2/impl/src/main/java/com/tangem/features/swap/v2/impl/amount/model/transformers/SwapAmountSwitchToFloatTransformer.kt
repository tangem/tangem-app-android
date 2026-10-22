package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapRateMode
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.utils.transformer.Transformer

internal class SwapAmountSwitchToFloatTransformer(
    private val isBalanceHidden: Boolean,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        val state = SwapAmountChangeAmountTypeTransformer(
            selectedAmountType = SwapAmountType.From,
            swapRateType = ExpressRateType.Float,
            isBalanceHidden = isBalanceHidden,
        ).transform(prevState)
        return (state as? SwapAmountUM.Content)?.copy(swapRateMode = SwapRateMode.FLOAT_ONLY) ?: state
    }
}