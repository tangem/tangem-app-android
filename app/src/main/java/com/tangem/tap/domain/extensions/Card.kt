package com.tangem.tap.domain.extensions

import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.extensions.toHexString
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.features.wallet.redux.Artwork

fun Card.getSingleWallet(): CardWallet? {
    return wallets.firstOrNull()
}

fun Card.hasWallets(): Boolean = wallets.isNotEmpty()

fun Card.hasNoWallets(): Boolean = wallets.isEmpty()

fun Card.hasSingleWallet(): Boolean = wallets.size == 1

fun Card.hasSignedHashes(): Boolean {
    return wallets.any { it.totalSignedHashes ?: 0 > 0 }
}

fun Card.signedHashesCount(): Int {
    return wallets.map { it.totalSignedHashes ?: 0 }.sum()
}

fun Card.getArtworkUrl(artworkId: String?): String? {
    return when {
        artworkId != null -> {
            OnlineCardVerifier.getUrlForArtwork(cardId, cardPublicKey.toHexString(), artworkId)
        }
        cardId.startsWith(Artwork.SERGIO_CARD_ID) -> Artwork.SERGIO_CARD_URL
        cardId.startsWith(Artwork.MARTA_CARD_ID) -> Artwork.MARTA_CARD_URL
        else -> null
    }
}

val Card.remainingSignatures: Int?
    get() = this.getSingleWallet()?.remainingSignatures

val Card.isWalletDataSupported: Boolean
    get() = this.firmwareVersion.major >= 4