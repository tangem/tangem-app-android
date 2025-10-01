package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

internal class TangemPayDetailsRefreshTransformer(private val isRefreshing: Boolean) : Transformer<TangemPayDetailsUM> {
    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        return prevState.copy(pullToRefreshConfig = prevState.pullToRefreshConfig.copy(isRefreshing = isRefreshing))
    }
}