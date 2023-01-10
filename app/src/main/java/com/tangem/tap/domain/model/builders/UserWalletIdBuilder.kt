package com.tangem.tap.domain.model.builders

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.Secp256k1
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ProductType
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.extensions.calculateHmacSha256
import com.tangem.domain.common.util.UserWalletId

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
        } else null

        return seed?.let {
            UserWalletId(value = calculateUserWalletId(it))
        }
    }

    private fun calculateUserWalletId(seed: ByteArray?): ByteArray? {
        val message = MESSAGE_FOR_WALLET_ID.toByteArray()
        val keyHash = seed?.calculateSha256()

        return if (keyHash != null) {
            message.calculateHmacSha256(keyHash)
        } else null
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

        fun scanResponse(scanResponse: ScanResponse): UserWalletIdBuilder {
            return UserWalletIdBuilder(
                publicKey = findPublicKey(scanResponse.card.wallets),
                pairTwinPublicKey = when (scanResponse.productType) {
                    ProductType.Twins -> scanResponse.secondTwinPublicKey?.hexToBytes()
                    ProductType.Note,
                    ProductType.Wallet,
                    ProductType.SaltPay,
                    ProductType.Start2Coin,
                    -> null
                },
            )
        }

        private fun findPublicKey(wallets: List<CardDTO.Wallet>): ByteArray? {
            return wallets.firstOrNull()
                ?.publicKey
        }
    }
}
