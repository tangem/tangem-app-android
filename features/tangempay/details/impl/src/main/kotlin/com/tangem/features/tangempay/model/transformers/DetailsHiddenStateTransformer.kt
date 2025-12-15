package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.entity.TangemPayCardDetailsBlockStateFactory
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.utils.transformer.Transformer

internal class DetailsHiddenStateTransformer(
    private val stateFactory: TangemPayCardDetailsBlockStateFactory,
) : Transformer<TangemPayCardDetailsUM> {

    override fun transform(prevState: TangemPayCardDetailsUM): TangemPayCardDetailsUM {
        val initialState = stateFactory.getInitialState()
        return prevState.copy(
            buttonText = initialState.buttonText,
            onClick = initialState.onClick,
            isHidden = initialState.isHidden,
            isLoading = initialState.isLoading,
        )
    }
}