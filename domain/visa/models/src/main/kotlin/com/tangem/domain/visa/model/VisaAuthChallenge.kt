package com.tangem.domain.visa.model

data class VisaAuthChallenge(
    val challenge: String,
    val session: VisaAuthSession,
)