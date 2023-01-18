package com.tangem.tap.domain.extensions

import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTangemNote
import com.tangem.domain.common.TwinCardNumber
import com.tangem.domain.common.getTwinCardNumber
import com.tangem.domain.common.isTangemTwin
import com.tangem.operations.attestation.CardVerifyAndGetInfo
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.features.wallet.redux.Artwork

val CardDTO.remainingSignatures: Int?
    get() = this.wallets.firstOrNull()?.remainingSignatures

val CardDTO.isWalletDataSupported: Boolean
    get() = this.firmwareVersion.major >= 4

val CardDTO.isMultiwalletAllowed: Boolean
    get() {
        return !isTangemTwin() && !isStart2Coin && !isTangemNote && !isSaltPay &&
            (firmwareVersion >= FirmwareVersion.MultiWalletAvailable ||
                wallets.firstOrNull()?.curve == EllipticCurve.Secp256k1)
    }

val CardDTO.isHdWalletAllowedByApp: Boolean
    get() = settings.isHDWalletAllowed && !isSaltPay

val CardDTO.isTangemWallet: Boolean
    get() = settings.isBackupAllowed
        && settings.isHDWalletAllowed
        && firmwareVersion >= FirmwareVersion.MultiWalletAvailable
        && !isSaltPay

@Suppress("UnnecessaryParentheses")
fun CardDTO.hasSignedHashes(): Boolean {
    return wallets.any { (it.totalSignedHashes ?: 0) > 0 }
}

fun CardDTO.signedHashesCount(): Int {
    return wallets.sumOf { it.totalSignedHashes ?: 0 }
}

suspend fun CardDTO.getOrLoadCardArtworkUrl(cardInfo: Result<CardVerifyAndGetInfo.Response.Item>? = null): String {
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

fun CardDTO.getArtworkUrl(artworkId: String?): String? {
    return when {
        artworkId != null -> {
            OnlineCardVerifier.getUrlForArtwork(cardId, cardPublicKey.toHexString(), artworkId)
        }

        cardId.startsWith(Artwork.SERGIO_CARD_ID) -> Artwork.SERGIO_CARD_URL
        cardId.startsWith(Artwork.MARTA_CARD_ID) -> Artwork.MARTA_CARD_URL
        else -> null
    }
}
