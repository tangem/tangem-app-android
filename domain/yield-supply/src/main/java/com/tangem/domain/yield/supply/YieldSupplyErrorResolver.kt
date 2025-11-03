package com.tangem.domain.yield.supply

interface YieldSupplyErrorResolver {
    fun resolve(throwable: Throwable): YieldSupplyError
}