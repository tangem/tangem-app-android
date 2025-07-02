package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.utils.transformer.Transformer

internal class SendDestinationPredefinedStateTransformer(
    private val address: String,
    private val memo: String?,
) : Transformer<DestinationUM> {

    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        return state.copy(
            addressTextField = state.addressTextField.copy(value = address, isValuePasted = false),
            memoTextField = state.memoTextField?.copy(value = memo.orEmpty(), isValuePasted = false),
        )
    }
}