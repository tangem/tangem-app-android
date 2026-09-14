package com.tangem.domain.wallets.builder

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.Secp256k1
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.utils.extensions.calculateHmacSha256
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWalletId

class UserWalletIdBuilder private constructor(
    private val publicKey: ByteArray?,
    private val pairTwinPublicKey: ByteArray? = null,
) {
    fun build(): UserWalletId? {
        val seed = if (publicKey != null) {
            if (pairTwinPublicKey != null) {
                Secp256k1.sum(publicKey, pairTwinPublicKey)
            } else {
                publicKey
            }
        } else {
            null
        }

        return seed?.let {
            UserWalletId(value = calculateUserWalletId(it))
        }
    }

    private fun calculateUserWalletId(seed: ByteArray?): ByteArray? {
        val message = MESSAGE_FOR_WALLET_ID.toByteArray()
        val keyHash = seed?.calculateSha256()

        return if (keyHash != null) {
            message.calculateHmacSha256(keyHash)
        } else {
            null
        }
    }

    companion object {
        private const val MESSAGE_FOR_WALLET_ID = "UserWalletID"

        @Throws(IllegalArgumentException::class)
        fun card(card: CardDTO): UserWalletIdBuilder {
            require(!card.isTangemTwins) {
                "For twin cards use scanResponse to ID calculation"
            }

            return UserWalletIdBuilder(findPublicKey(card.wallets))
        }

        /**
         * Builder for a physically read [card] that is expected to belong to the wallet described by
         * [walletScanResponse]. Unlike [card], supports twin cards: a twin's ID is derived from both cards' keys,
         * so the pair key is taken from the wallet's scan response. A card of another wallet yields a different ID.
         */
        fun card(card: CardDTO, walletScanResponse: ScanResponse): UserWalletIdBuilder {
            val cardPublicKey = findPublicKey(card.wallets)
            if (!card.isTangemTwins) return UserWalletIdBuilder(cardPublicKey)

            val firstTwinPublicKey = findPublicKey(walletScanResponse.card.wallets)
            val secondTwinPublicKey = walletScanResponse.secondTwinPublicKey?.hexToBytes()
            val isFirstTwin = cardPublicKey != null && cardPublicKey.contentEquals(firstTwinPublicKey)

            return UserWalletIdBuilder(
                publicKey = cardPublicKey,
                pairTwinPublicKey = if (isFirstTwin) secondTwinPublicKey else firstTwinPublicKey,
            )
        }

        fun scanResponse(scanResponse: ScanResponse): UserWalletIdBuilder {
            return UserWalletIdBuilder(
                publicKey = findPublicKey(scanResponse.card.wallets),
                pairTwinPublicKey = when (scanResponse.productType) {
                    ProductType.Twins -> scanResponse.secondTwinPublicKey?.hexToBytes()
                    ProductType.Note,
                    ProductType.Wallet,
                    ProductType.Wallet2,
                    ProductType.Ring,
                    ProductType.Start2Coin,
                    ProductType.Visa,
                    -> null
                },
            )
        }

        fun walletPublicKey(publicKey: ByteArray): UserWalletId {
            return UserWalletIdBuilder(
                publicKey = publicKey,
            ).build()!!
        }

        private fun findPublicKey(wallets: List<CardDTO.Wallet>): ByteArray? {
            return wallets.firstOrNull()
                ?.publicKey
        }
    }
}