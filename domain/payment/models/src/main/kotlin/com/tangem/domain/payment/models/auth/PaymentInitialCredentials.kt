package com.tangem.domain.payment.models.auth

data class PaymentInitialCredentials(
    val customerWalletAddress: String,
    val authTokens: PaymentAuthTokens,
)