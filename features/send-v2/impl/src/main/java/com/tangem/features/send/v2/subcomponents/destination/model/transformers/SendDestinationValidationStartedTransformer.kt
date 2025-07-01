package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.utils.transformer.Transformer

internal object SendDestinationValidationStartedTransformer : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        return state.copy(isValidating = true)
    }
}