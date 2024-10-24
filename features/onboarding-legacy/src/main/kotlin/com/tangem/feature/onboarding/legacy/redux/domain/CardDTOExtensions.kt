package com.tangem.feature.onboarding.legacy.redux.domain

import com.tangem.domain.common.TwinCardNumber
import com.tangem.domain.common.TwinsHelper
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.models.Artwork
import com.tangem.common.services.Result
import com.tangem.feature.onboarding.legacy.redux.common.toHexString
import com.tangem.operations.attestation.CardVerifyAndGetInfo
import com.tangem.operations.attestation.OnlineCardVerifier

internal fun CardDTO.signedHashesCount(): Int {
    return wallets.sumOf { it.totalSignedHashes ?: 0 }
}

suspend fun CardDTO.getOrLoadCardArtworkUrl(cardInfo: Result<CardVerifyAndGetInfo.Response.Item>? = null): String {
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

    return when (val cardInfoResult = cardInfo ?: OnlineCardVerifier().getCardInfo(cardId, cardPublicKey)) {
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

fun CardDTO.getTwinCardNumber(): TwinCardNumber? {
    return TwinsHelper.getTwinCardNumber(this.cardId)
}
