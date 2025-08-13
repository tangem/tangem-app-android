package com.tangem.tap.domain.userWalletList.utils

import com.tangem.common.extensions.calculateSha256
import com.tangem.domain.common.extensions.calculateHmacSha256
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet

internal val UserWallet.encryptionKey: ByteArray?
    get() = when (this) {
        is UserWallet.Cold -> findPublicKey(this.scanResponse.card.wallets)
        is UserWallet.Hot -> findPublicKey(this.wallets.orEmpty())
    }?.let { calculateEncryptionKey(it) }

private fun calculateEncryptionKey(publicKey: ByteArray): ByteArray {
    val message = MESSAGE_FOR_ENCRYPTION_KEY.toByteArray()
    val keyHash = publicKey.calculateSha256()

    return message.calculateHmacSha256(keyHash)
}

private fun findPublicKey(wallets: List<CardDTO.Wallet>): ByteArray? {
    return wallets.firstOrNull()?.publicKey
}

@JvmName("findPublicKeyInMobileWallets")
private fun findPublicKey(wallets: List<MobileWallet>): ByteArray? {
    return wallets.firstOrNull()?.publicKey
}

private const val MESSAGE_FOR_ENCRYPTION_KEY = "UserWalletEncryptionKey"