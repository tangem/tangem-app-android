package com.tangem.domain.pay.model

enum class OrderStatus {
    NEW,
    PROCESSING,
    COMPLETED,
    CANCELED,
    ;

    /** An order is active while it is still being processed (NEW or PROCESSING). */
    val isActive: Boolean get() = activeStatuses.contains(this)

    /** Terminal statuses (COMPLETED or CANCELED) — used to invalidate the local order hint. */
    val isTerminal: Boolean get() = terminalStatuses.contains(this)

    companion object {
        /** Statuses of an in-flight order (still being processed). */
        val activeStatuses: Set<OrderStatus> = setOf(NEW, PROCESSING)

        /** Statuses of a finished order — no further state changes are expected. */
        val terminalStatuses: Set<OrderStatus> = setOf(COMPLETED, CANCELED)
    }
}