package com.tangem.tap.domain.userWalletList.utils

import com.tangem.common.extensions.calculateSha256
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.extensions.calculateHmacSha256

internal val CardDTO.encryptionKey: ByteArray?
    get() = findPublicKey(wallets)?.let { calculateEncryptionKey(it) }

private fun calculateEncryptionKey(publicKey: ByteArray): ByteArray {
    val message = MESSAGE_FOR_ENCRYPTION_KEY.toByteArray()
    val keyHash = publicKey.calculateSha256()

    return message.calculateHmacSha256(keyHash)
}

private fun findPublicKey(wallets: List<CardDTO.Wallet>): ByteArray? {
    return wallets.firstOrNull()
        ?.publicKey
}

private const val MESSAGE_FOR_ENCRYPTION_KEY = "UserWalletEncryptionKey"
