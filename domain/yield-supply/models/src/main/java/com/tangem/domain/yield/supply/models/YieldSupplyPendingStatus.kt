package com.tangem.domain.yield.supply.models

sealed class YieldSupplyPendingStatus(open val txIds: List<String>) {
    data class Enter(override val txIds: List<String>) : YieldSupplyPendingStatus(txIds)
    data class Exit(override val txIds: List<String>) : YieldSupplyPendingStatus(txIds)
}