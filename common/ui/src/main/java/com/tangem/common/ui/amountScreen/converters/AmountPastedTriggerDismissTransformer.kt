package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.utils.transformer.Transformer

/**
 * Dismisses indication on pasted value
 */
class AmountPastedTriggerDismissTransformer : Transformer<AmountState> {
    override fun transform(prevState: AmountState): AmountState {
        if (prevState !is AmountState.Data) return prevState

        return prevState.copy(
            amountTextField = prevState.amountTextField.copy(
                isValuePasted = false,
            ),
        )
    }
}