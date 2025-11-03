package com.tangem.features.yield.supply.impl.common.entity.transformer

import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.utils.transformer.Transformer

internal object YieldSupplyTransactionInProgressTransformer : Transformer<YieldSupplyActionUM> {
    override fun transform(prevState: YieldSupplyActionUM): YieldSupplyActionUM {
        return prevState.copy(
            isPrimaryButtonEnabled = false,
            isTransactionSending = true,
        )
    }
}