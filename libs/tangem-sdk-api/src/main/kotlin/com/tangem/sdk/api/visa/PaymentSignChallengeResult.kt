package com.tangem.sdk.api.visa

import com.tangem.domain.payment.models.auth.PaymentAuthChallenge

data class PaymentSignChallengeResult(
    val address: String,
    val challenge: PaymentAuthChallenge,
    val signature: String,
)