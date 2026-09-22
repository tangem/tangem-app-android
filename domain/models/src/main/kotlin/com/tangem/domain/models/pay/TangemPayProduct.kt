package com.tangem.domain.models.pay

enum class TangemPayProduct {
    BANKING_ACCOUNT,
    PAYMENT_ACCOUNT,
}

val TangemPayProduct.id: String
    get() = when (this) {
        TangemPayProduct.BANKING_ACCOUNT -> "BANKING_ACCOUNT"
        TangemPayProduct.PAYMENT_ACCOUNT -> "PAYMENT_ACCOUNT"
    }