package com.tangem.domain.wallets.usecase

import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.domain.common.TwinCardNumber
import com.tangem.domain.common.TwinsHelper
import com.tangem.domain.wallets.models.Artwork.Companion.DEFAULT_IMG_URL
import com.tangem.domain.wallets.models.Artwork.Companion.MARTA_CARD_ID
import com.tangem.domain.wallets.models.Artwork.Companion.MARTA_CARD_URL
import com.tangem.domain.wallets.models.Artwork.Companion.SERGIO_CARD_ID
import com.tangem.domain.wallets.models.Artwork.Companion.SERGIO_CARD_URL
import com.tangem.domain.wallets.models.Artwork.Companion.TWIN_CARD_1
import com.tangem.domain.wallets.models.Artwork.Companion.TWIN_CARD_2
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.operations.attestation.TangemApi

/**
 * Use case for getting card image url
 *
 * @property verifier REST API
 *
 * @author Andrew Khokhlov on 12/04/2023
 */
class GetCardImageUseCase(private val verifier: OnlineCardVerifier = OnlineCardVerifier()) {

    /**
     * Get card image url
     *
     * @param cardId        card id
     * @param cardPublicKey card public key
     */
    suspend operator fun invoke(cardId: String, cardPublicKey: ByteArray): String {
        return when (val result = verifier.getCardInfo(cardId, cardPublicKey)) {
            is Result.Success -> {
                val artworkId = result.data.artwork?.id
                if (artworkId.isNullOrEmpty()) {
                    getFallbackArtworkUrl(cardId)
                } else {
                    getUrlForArtwork(cardId, cardPublicKey.toHexString(), artworkId)
                }
            }

            is Result.Failure -> getFallbackArtworkUrl(cardId)
        }
    }

    private fun getFallbackArtworkUrl(cardId: String): String {
        return when {
            cardId.startsWith(SERGIO_CARD_ID) -> SERGIO_CARD_URL
            cardId.startsWith(MARTA_CARD_ID) -> MARTA_CARD_URL
            else -> when (TwinsHelper.getTwinCardNumber(cardId)) {
                TwinCardNumber.First -> TWIN_CARD_1
                TwinCardNumber.Second -> TWIN_CARD_2
                else -> DEFAULT_IMG_URL
            }
        }
    }

    private fun getUrlForArtwork(cardId: String, cardPublicKeyHex: String, artworkId: String): String {
        return TangemApi.Companion.BaseUrl.VERIFY.url + TangemApi.ARTWORK +
            "?artworkId=$artworkId&CID=$cardId&publicKey=$cardPublicKeyHex"
    }
}
