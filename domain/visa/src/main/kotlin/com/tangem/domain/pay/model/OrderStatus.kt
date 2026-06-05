package com.tangem.domain.pay.model

import java.util.Locale

enum class OrderStatus {
    NEW,
    PROCESSING,
    COMPLETED,
    CANCELED,
    ;

    override fun toString() = when (this) {
        NEW -> "New"
        PROCESSING -> "Processing"
        COMPLETED -> "Completed"
        CANCELED -> "Canceled"
    }

    companion object {
        fun fromString(value: String) = when (value.lowercase(Locale.US)) {
            "processing" -> PROCESSING
            "completed" -> COMPLETED
            "canceled" -> CANCELED
            else -> NEW
        }
    }
}

val OrderStatus.isFinalStatus
    get() = this == OrderStatus.COMPLETED || this == OrderStatus.CANCELED