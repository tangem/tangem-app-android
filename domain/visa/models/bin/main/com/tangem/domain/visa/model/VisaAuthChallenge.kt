package com.tangem.domain.visa.model

sealed class VisaAuthChallenge {

    abstract val session: VisaAuthSession

    data class Card(
        val challenge: String,
        override val session: VisaAuthSession,
    ) : VisaAuthChallenge()

    data class Wallet(
        val challenge: String,
        override val session: VisaAuthSession,
    ) : VisaAuthChallenge()
}

fun VisaAuthChallenge.Card.toSignedChallenge(signedChallenge: String, salt: String): VisaAuthSignedChallenge {
    return VisaAuthSignedChallenge.ByCardPublicKey(
        challenge = this,
        signature = signedChallenge,
        salt = salt,
    )
}

fun VisaAuthChallenge.Wallet.toSignedChallenge(signedChallenge: String): VisaAuthSignedChallenge {
    return VisaAuthSignedChallenge.ByWallet(
        challenge = this,
        signature = signedChallenge,
    )
}