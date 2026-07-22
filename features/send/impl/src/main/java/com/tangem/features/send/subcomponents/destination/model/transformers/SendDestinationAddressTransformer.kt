package com.tangem.features.send.subcomponents.destination.model.transformers

import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.utils.transformer.Transformer

internal class SendDestinationAddressTransformer(
    private val address: String,
    private val isPasted: Boolean,
) : Transformer<DestinationUM> {

    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        return state.copy(
            addressTextField = state.addressTextField.copy(
                value = address,
                isValuePasted = isPasted,
                // Editing the recipient invalidates any previously resolved/recognized data. It is
                // recomputed by the following validation; keeping it would let a stale canonical
                // address (blockchainAddress) leak into the transaction for the new, not-yet-validated value.
                blockchainAddress = null,
                contactName = null,
                contactIcon = null,
            ),
        )
    }
}