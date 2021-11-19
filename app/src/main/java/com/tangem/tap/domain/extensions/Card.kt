package com.tangem.tap.domain.extensions

import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.operations.attestation.CardVerifyAndGetInfo
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.domain.twins.getTwinCardNumber
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

suspend fun Card.getOrLoadCardArtworkUrl(cardInfo: Result<CardVerifyAndGetInfo.Response.Item>? = null): String {
    fun ifAnyError(): String {
        return when {
            cardId.startsWith(Artwork.SERGIO_CARD_ID) -> Artwork.SERGIO_CARD_URL
            cardId.startsWith(Artwork.MARTA_CARD_ID) -> Artwork.MARTA_CARD_URL
            else -> {
                when (getTwinCardNumber()) {
                    TwinCardNumber.First -> Artwork.TWIN_CARD_1
                    TwinCardNumber.Second -> Artwork.TWIN_CARD_2
                    else -> Artwork.DEFAULT_IMG_URL
                }
            }
        }
    }

    val cardInfoResult = cardInfo ?: OnlineCardVerifier().getCardInfo(cardId, cardPublicKey)
    return when (cardInfoResult) {
        is Result.Success -> {
            val artworkId = cardInfoResult.data.artwork?.id
            if (artworkId == null || artworkId.isEmpty()) {
                ifAnyError()
            } else {
                OnlineCardVerifier.getUrlForArtwork(cardId, cardPublicKey.toHexString(), artworkId)
            }
        }
        is Result.Failure -> ifAnyError()
    }
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