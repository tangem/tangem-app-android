package com.tangem.tap.domain.model.builders

import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ProductType
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TwinCardNumber
import com.tangem.domain.common.TwinsHelper
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.operations.attestation.TangemApi
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.features.wallet.redux.Artwork

class UserWalletBuilder(
    private val scanResponse: ScanResponse,
    private val onlineCardVerifier: OnlineCardVerifier = OnlineCardVerifier(),
) {
    private var backupCardsIds: Set<String> = emptySet()

    private val CardDTO.isBackupNotAllowed: Boolean
        get() = !this.settings.isBackupAllowed

    private val ScanResponse.userWalletName: String
        get() = when (productType) {
            ProductType.Note -> "Note"
            ProductType.Twins -> "Twin"
            ProductType.SaltPay -> "SaltPay"
            ProductType.Start2Coin -> "Start2Coin"
            ProductType.Wallet -> when {
                card.isBackupNotAllowed -> "Tangem card"
                card.isStart2Coin -> "Start2Coin"
                else -> "Wallet"
            }
        }

    fun backupCardsIds(backupCardsIds: Set<String>?) = this.apply {
        if (backupCardsIds != null) {
            this.backupCardsIds = backupCardsIds
        }
    }

    suspend fun build(): UserWallet? {
        return with(scanResponse) {
            UserWalletIdBuilder.scanResponse(scanResponse)
                .build()
                ?.let {
                    UserWallet(
                        walletId = it,
                        name = userWalletName,
                        artworkUrl = loadArtworkUrl(card.cardId, card.cardPublicKey),
                        cardsInWallet = backupCardsIds.plus(card.cardId),
                        scanResponse = this,
                        isMultiCurrency = cardTypesResolver.isMultiwalletAllowed(),
                    )
                }
        }
    }

    private suspend fun loadArtworkUrl(cardId: String, cardPublicKey: ByteArray): String {
        return when (val result = onlineCardVerifier.getCardInfo(cardId, cardPublicKey)) {
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
            cardId.startsWith(Artwork.SERGIO_CARD_ID) -> Artwork.SERGIO_CARD_URL
            cardId.startsWith(Artwork.MARTA_CARD_ID) -> Artwork.MARTA_CARD_URL
            else -> when (TwinsHelper.getTwinCardNumber(cardId)) {
                TwinCardNumber.First -> Artwork.TWIN_CARD_1
                TwinCardNumber.Second -> Artwork.TWIN_CARD_2
                else -> Artwork.DEFAULT_IMG_URL
            }
        }
    }

    private fun getUrlForArtwork(cardId: String, cardPublicKeyHex: String, artworkId: String): String {
        return TangemApi.Companion.BaseUrl.VERIFY.url + TangemApi.ARTWORK +
            "?artworkId=$artworkId&CID=$cardId&publicKey=$cardPublicKeyHex"
    }
}
