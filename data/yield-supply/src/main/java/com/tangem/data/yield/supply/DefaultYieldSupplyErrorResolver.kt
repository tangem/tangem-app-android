package com.tangem.data.yield.supply

import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver

internal data object DefaultYieldSupplyErrorResolver : YieldSupplyErrorResolver {
    override fun resolve(throwable: Throwable): YieldSupplyError {
        return when (throwable) {
            is YieldSupplyError -> throwable
            is YieldModuleUpgradeUnavailableException -> YieldSupplyError.ModuleUpgradeUnavailable(
                currentImplementation = throwable.currentImplementation,
            )
            else -> YieldSupplyError.DataError(throwable)
        }
    }
}