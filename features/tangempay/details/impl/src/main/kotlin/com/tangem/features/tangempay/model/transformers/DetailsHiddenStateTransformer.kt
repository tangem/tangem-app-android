package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.entity.TangemPayDetailsStateFactory
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

internal class DetailsHiddenStateTransformer(
    private val stateFactory: TangemPayDetailsStateFactory,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        return prevState.copy(cardDetailsUM = stateFactory.getInitialState().cardDetailsUM)
    }
}