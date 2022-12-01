package com.tangem.tap.domain.userWalletList.model

import com.squareup.moshi.JsonClass
import com.tangem.domain.common.util.UserWalletId
import com.tangem.domain.common.util.encryptionKey
import com.tangem.tap.domain.model.UserWallet

@JsonClass(generateAdapter = true)
internal data class UserWalletEncryptionKey(
    val walletId: UserWalletId,
    val encryptionKey: ByteArray,
) {
    constructor(userWallet: UserWallet) : this(
        walletId = userWallet.walletId,
        encryptionKey = userWallet.scanResponse.card.encryptionKey,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserWalletEncryptionKey) return false

        if (walletId != other.walletId) return false
        if (!encryptionKey.contentEquals(other.encryptionKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = walletId.hashCode()
        result = 31 * result + encryptionKey.contentHashCode()
        return result
    }
}
