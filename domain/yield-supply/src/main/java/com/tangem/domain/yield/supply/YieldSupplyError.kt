package com.tangem.domain.yield.supply

sealed class YieldSupplyError : Throwable() {

    abstract val code: Int

    data class DataError(
        val throwable: Throwable,
    ) : YieldSupplyError() {
        override val code: Int = -1
    }
}