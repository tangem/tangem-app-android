package com.tangem.domain.pay.model

enum class OrderStatus {
    UNKNOWN, // TODO remove it after TangemPay accounts refactor TANGEM_PAY_ACCOUNTS_REFACTOR_ENABLED
    NEW,
    PROCESSING,
    COMPLETED,
    CANCELED,
}