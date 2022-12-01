package com.tangem.domain.common.util

import com.tangem.common.extensions.calculateSha256
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.extensions.calculateHmacSha256

val CardDTO.userWalletId: UserWalletId
    get() = UserWalletId(findWalletPublicKey(wallets))

val CardDTO.encryptionKey: ByteArray
    get() = findWalletPublicKey(wallets)
        ?.let { calculateEncryptionKey(it) }
        ?: error("Wallet ID not found")

private fun calculateEncryptionKey(publicKey: ByteArray): ByteArray {
    val message = MESSAGE_FOR_ENCRYPTION_KEY.toByteArray()
    val keyHash = publicKey.calculateSha256()
    return message.calculateHmacSha256(keyHash)
}

private fun findWalletPublicKey(wallets: List<CardDTO.Wallet>): ByteArray? {
    return wallets.firstOrNull()
        ?.publicKey
}

private const val MESSAGE_FOR_ENCRYPTION_KEY = "UserWalletEncryptionKey"
