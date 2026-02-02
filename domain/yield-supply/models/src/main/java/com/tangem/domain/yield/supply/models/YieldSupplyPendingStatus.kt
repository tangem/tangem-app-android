package com.tangem.domain.yield.supply.models

sealed class YieldSupplyPendingStatus(open val txIds: List<String>, open val createdAt: Long) {
    data class Enter(
        override val txIds: List<String>,
        override val createdAt: Long = System.currentTimeMillis(),
    ) : YieldSupplyPendingStatus(txIds, createdAt)

    data class Exit(
        override val txIds: List<String>,
        override val createdAt: Long = System.currentTimeMillis(),
    ) : YieldSupplyPendingStatus(txIds, createdAt)
}