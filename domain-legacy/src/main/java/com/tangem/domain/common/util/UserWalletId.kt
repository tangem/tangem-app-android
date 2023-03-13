package com.tangem.domain.common.util

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString

class UserWalletId(
    val stringValue: String,
) {
    val value = stringValue.hexToBytes()

    constructor(value: ByteArray?) : this(
        stringValue = value?.toHexString() ?: "",
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

    @Suppress("MagicNumber")
    override fun toString(): String {
        return with(stringValue) {
            "UserWalletId(${take(3)}...${takeLast(3)})"
        }
    }
}
