package com.tangem.tap.domain.userWalletList.model

import com.squareup.moshi.JsonClass
import com.tangem.domain.wallets.models.UserWalletId

@JsonClass(generateAdapter = true)
internal data class UserWalletEncryptionKey(
    val walletId: UserWalletId,
    val encryptionKey: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserWalletEncryptionKey) return false

        if (walletId != other.walletId) return false
        return encryptionKey.contentEquals(other.encryptionKey)
    }

    override fun hashCode(): Int {
        var result = walletId.hashCode()
        result = 31 * result + encryptionKey.contentHashCode()
        return result
    }
}