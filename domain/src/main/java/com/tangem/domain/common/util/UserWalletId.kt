package com.tangem.domain.common.util

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.domain.common.extensions.calculateHmacSha256

class UserWalletId(
    val stringValue: String,
) {
    val value = stringValue.hexToBytes()

    constructor(walletPublicKey: ByteArray?) : this(
        stringValue = walletPublicKey?.let { calculateUserWalletId(it).toHexString() } ?: "",
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserWalletId) return false

        if (stringValue != other.stringValue) return false

        return true
    }

    override fun hashCode(): Int {
        return stringValue.hashCode()
    }

    override fun toString(): String {
        return with(stringValue) {
            "UserWalletId(${take(3)}...${takeLast(3)})"
        }
    }
}

private fun calculateUserWalletId(publicKey: ByteArray): ByteArray {
    val message = MESSAGE_FOR_WALLET_ID.toByteArray()
    val keyHash = publicKey.calculateSha256()
    return message.calculateHmacSha256(keyHash)
}

private const val MESSAGE_FOR_WALLET_ID = "UserWalletID"
