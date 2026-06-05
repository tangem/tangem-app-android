package com.tangem.domain.pay.model

import com.tangem.domain.models.wallet.UserWalletId
import java.util.Locale

data class TangemPayPendingOrder(
    val orderId: String,
    val userWalletId: UserWalletId,
    val cardId: String,
    val type: Type,
    val status: OrderStatus,
) {
    enum class Type {
        UNKNOWN,
        REISSUE,
        FREEZE,
        UNFREEZE,
        CLOSE,
        ;

        override fun toString() = when (this) {
            UNKNOWN -> "Unknown"
            REISSUE -> "Reissue"
            FREEZE -> "Freeze"
            UNFREEZE -> "Unfreeze"
            CLOSE -> "Close"
        }

        companion object {
            fun fromString(value: String) = when (value.lowercase(Locale.US)) {
                "reissue" -> REISSUE
                "unfreeze" -> UNFREEZE
                "close" -> CLOSE
                "freeze" -> FREEZE
                else -> UNKNOWN
            }
        }
    }
}

fun List<TangemPayPendingOrder>.hasOngoingOrders(cardId: String, vararg types: TangemPayPendingOrder.Type): Boolean {
    return any { it.cardId == cardId && !it.status.isFinalStatus && it.type in types }
}