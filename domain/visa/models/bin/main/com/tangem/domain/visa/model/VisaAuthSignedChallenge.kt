package com.tangem.domain.visa.model

sealed class VisaAuthSignedChallenge {

    data class ByCardPublicKey(
        val challenge: VisaAuthChallenge.Card,
        val signature: String,
        val salt: String,
    ) : VisaAuthSignedChallenge()

    data class ByWallet(
        val challenge: VisaAuthChallenge.Wallet,
        val signature: String,
    ) : VisaAuthSignedChallenge()
}