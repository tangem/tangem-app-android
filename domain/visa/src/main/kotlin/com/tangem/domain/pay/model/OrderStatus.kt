package com.tangem.domain.pay.model

enum class OrderStatus {
    NEW,
    PROCESSING,
    COMPLETED,
    CANCELED,
    ;

    /** An order is active while it is still being processed (NEW or PROCESSING). */
    val isActive: Boolean get() = this == NEW || this == PROCESSING

    /** Terminal statuses (COMPLETED or CANCELED) — used to invalidate the local order hint. */
    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELED
}