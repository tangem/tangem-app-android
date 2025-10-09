package com.tangem.features.yield.supply.impl.main.model.transformers

import com.tangem.features.yield.supply.impl.main.entity.LoadingStatusMode
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.utils.transformer.Transformer

internal class YieldSupplyTokenStatusFailureTransformer(
    private val mode: LoadingStatusMode,
) : Transformer<YieldSupplyUM> {

    override fun transform(prevState: YieldSupplyUM): YieldSupplyUM {
        return when (mode) {
            LoadingStatusMode.Initial -> YieldSupplyUM.Unavailable
            LoadingStatusMode.LoadApy -> prevState // TODO apply correct UI
        }
    }
}