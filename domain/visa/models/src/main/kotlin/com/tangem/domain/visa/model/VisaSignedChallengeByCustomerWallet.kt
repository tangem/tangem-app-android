package com.tangem.domain.visa.model

data class VisaSignedChallengeByCustomerWallet(
    val challenge: String,
    val signature: String,
)