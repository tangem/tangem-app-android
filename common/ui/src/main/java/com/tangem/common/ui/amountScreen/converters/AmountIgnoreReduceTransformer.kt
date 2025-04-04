package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.utils.transformer.Transformer

object AmountIgnoreReduceTransformer : Transformer<AmountState> {
    override fun transform(prevState: AmountState): AmountState {
        val state = prevState as? AmountState.Data ?: return prevState
        return state.copy(isIgnoreReduce = true)
    }
}