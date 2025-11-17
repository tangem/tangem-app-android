package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

internal class DetailBalanceVisibilityTransformer(
    private val isHidden: Boolean,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        return prevState.copy(isBalanceHidden = isHidden)
    }
}