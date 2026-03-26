package com.tangem.domain.payment.models.auth

data class PaymentAuthChallenge(
    val challenge: String,
    val session: PaymentAuthSession,
)