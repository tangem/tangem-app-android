package com.tangem.features.send.subcomponents.destination.model.transformers

import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.utils.transformer.Transformer

internal object SendDestinationValidationStartedTransformer : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        // Disable the primary button while validation is pending so it cannot be pressed with a
        // stale enablement carried over from the previously validated value. It is re-enabled by
        // SendDestinationValidationResultTransformer only when the new value is valid.
        return state.copy(isValidating = true, isPrimaryButtonEnabled = false)
    }
}