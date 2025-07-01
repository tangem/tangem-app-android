package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.utils.transformer.Transformer

internal class SendDestinationMemoTransformer(
    private val memo: String,
    private val isPasted: Boolean,
) : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState
        return state.copy(
            memoTextField = state.memoTextField?.copy(value = memo, isValuePasted = isPasted),
        )
    }
}