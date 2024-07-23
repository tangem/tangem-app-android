package com.tangem.domain.wallets.models

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import kotlinx.serialization.Serializable

@Serializable
data class UserWalletId(val stringValue: String) {

    val value = stringValue.hexToBytes()

    constructor(value: ByteArray?) : this(stringValue = value?.toHexString() ?: "")

    override fun toString() = "UserWalletId(${stringValue.take(n = 3)}...${stringValue.takeLast(n = 3)})"
}
