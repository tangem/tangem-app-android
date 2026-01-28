package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.entity.TangemPayViewPinUM
import com.tangem.utils.transformer.Transformer

internal class TangemPayViewPinSuccessStateTransformer(
    private val pin: String,
    private val onClickChangePin: () -> Unit,
) : Transformer<TangemPayViewPinUM> {

    override fun transform(prevState: TangemPayViewPinUM): TangemPayViewPinUM {
        return TangemPayViewPinUM.Content(
            pin = pin,
            onClickChangePin = onClickChangePin,
            onDismiss = prevState.onDismiss,
        )
    }
}