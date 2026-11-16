package com.tangem.domain.visa.model

sealed class VisaAuthChallenge {

    abstract val session: VisaAuthSession

    data class Wallet(
        val challenge: String,
        override val session: VisaAuthSession,
    ) : VisaAuthChallenge()
}