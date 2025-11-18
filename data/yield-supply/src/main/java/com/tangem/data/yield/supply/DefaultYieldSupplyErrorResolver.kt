package com.tangem.data.yield.supply

import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver

internal data object DefaultYieldSupplyErrorResolver : YieldSupplyErrorResolver {
    override fun resolve(throwable: Throwable): YieldSupplyError {
        return when (throwable) {
            is YieldSupplyError -> throwable
            else -> YieldSupplyError.DataError(throwable)
        }
    }
}