package com.tangem.domain.walletconnect.model.pay

enum class WcPaymentStatus {
    REQUIRES_ACTION,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    EXPIRED,
    CANCELLED,
}