package com.tangem.data.txhistory.repository

/**
 * Identity of one express operation. The swap and the onramp id spaces do not intersect, but they live in
 * tables of their own, so the [type] is what says which one to read a status from.
 */
internal data class ExpressOperationId(
    val id: String,
    val type: ExpressOperationType,
)

internal enum class ExpressOperationType {
    Swap,
    Onramp,
}