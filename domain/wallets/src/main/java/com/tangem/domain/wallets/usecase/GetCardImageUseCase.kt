package com.tangem.domain.wallets.usecase

import com.tangem.common.card.FirmwareVersion
import com.tangem.common.services.Result
import com.tangem.domain.common.TwinCardNumber
import com.tangem.domain.common.TwinsHelper
import com.tangem.domain.models.ArtworkModel
import com.tangem.domain.wallets.models.Artwork
import com.tangem.operations.attestation.ArtworkSize
import com.tangem.operations.attestation.CardArtworksProvider

/**
 * Use case for getting card image url
 *
 * @property cardArtworksProvider card artworks provider
 *
[REDACTED_AUTHOR]
 */
class GetCardImageUseCase(
    private val cardArtworksProvider: CardArtworksProvider,
) {

    /**
     * Get card image url
     *
     * @param cardId        card id
     * @param cardPublicKey card public key
     */
    suspend operator fun invoke(
        cardId: String,
        cardPublicKey: ByteArray,
        manufacturerName: String,
        firmwareVersion: FirmwareVersion,
        size: ArtworkSize = ArtworkSize.SMALL,
    ): ArtworkModel {
        val result = cardArtworksProvider.getArtwork(
            cardId = cardId,
            cardPublicKey = cardPublicKey,
            manufacturerName = manufacturerName,
            firmwareVersion = firmwareVersion,
            size = size,
        )

        return when (result) {
            is Result.Failure -> ArtworkModel(null, getFallbackArtworkUrl(cardId))
            is Result.Success -> ArtworkModel(result.data, getFallbackArtworkUrl(cardId))
        }
    }

    private fun getFallbackArtworkUrl(cardId: String): String {
        return when {
            cardId.startsWith(Artwork.SERGIO_CARD_ID) -> Artwork.SERGIO_CARD_URL
            cardId.startsWith(Artwork.MARTA_CARD_ID) -> Artwork.MARTA_CARD_URL
            else -> when (TwinsHelper.getTwinCardNumber(cardId)) {
                TwinCardNumber.First -> Artwork.TWIN_CARD_1_URL
                TwinCardNumber.Second -> Artwork.TWIN_CARD_2_URL
                else -> Artwork.DEFAULT_IMG_URL
            }
        }
    }
}