package com.tangem.tap.domain.extensions

import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.domain.common.TwinCardNumber
import com.tangem.domain.common.getTwinCardNumber
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.models.Artwork
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.operations.attestation.api.models.CardVerifyAndGetInfo

fun CardDTO.signedHashesCount(): Int {
    return wallets.sumOf { it.totalSignedHashes ?: 0 }
}

suspend fun CardDTO.getOrLoadCardArtworkUrl(
    cardInfo: Result<CardVerifyAndGetInfo.Response.Item>? = null,
    onlineCardVerifier: OnlineCardVerifier,
): String {
    fun ifAnyError(): String {
        return when {
            cardId.startsWith(Artwork.SERGIO_CARD_ID) -> Artwork.SERGIO_CARD_URL
            cardId.startsWith(Artwork.MARTA_CARD_ID) -> Artwork.MARTA_CARD_URL
            else -> {
                when (getTwinCardNumber()) {
                    TwinCardNumber.First -> Artwork.TWIN_CARD_1_URL
                    TwinCardNumber.Second -> Artwork.TWIN_CARD_2_URL
                    else -> Artwork.DEFAULT_IMG_URL
                }
            }
        }
    }

    return when (val cardInfoResult = cardInfo ?: onlineCardVerifier.getCardInfo(cardId, cardPublicKey)) {
        is Result.Success -> {
            val artworkId = cardInfoResult.data.artwork?.id
            if (artworkId.isNullOrEmpty()) {
                ifAnyError()
            } else {
                OnlineCardVerifier.getUrlForArtwork(cardId, cardPublicKey.toHexString(), artworkId)
            }
        }

        is Result.Failure -> ifAnyError()
    }
}