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
    sealed interface Unavailable : PaymentMethodStatus {
        val requiredAmount: BigDecimal

        data class MinAmount(override val requiredAmount: BigDecimal) : Unavailable
        data class MaxAmount(override val requiredAmount: BigDecimal) : Unavailable
    }
}