package com.tangem.domain.models.userwallet

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import kotlin.random.Random

class UserWalletId(
    val stringValue: String,
) {

    val value: ByteArray = stringValue.hexToBytes()

    init {
        require(stringValue.isNotBlank() || value.isEmpty()) { "Incorrect user wallet ID" }
    }

    constructor(value: ByteArray) : this(
        stringValue = value.toHexString(),
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
            "UserWalletId(${take(n = 3)}...${takeLast(n = 3)})"
        }
    }

    companion object {
        fun mock(n: Int = Random.nextInt(from = 0, until = 1_000)): UserWalletId {
            return UserWalletId(stringValue = n.toString())
        }
    }
}
