package com.tangem.domain.onramp.model

import java.math.BigDecimal

data class OnrampPaymentMethodGroup(
    val paymentMethod: OnrampPaymentMethod,
    val offers: List<OnrampOffer>,
    val bestRateOffer: OnrampOffer?,
    val providerCount: Int,
    val isBestPaymentMethod: Boolean,
    val methodStatus: PaymentMethodStatus,
)

sealed interface PaymentMethodStatus {
    data object Available : PaymentMethodStatus
    data class Unavailable(val availableFrom: BigDecimal) : PaymentMethodStatus
}