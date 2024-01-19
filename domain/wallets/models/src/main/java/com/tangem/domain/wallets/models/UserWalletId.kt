package com.tangem.domain.wallets.models

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import java.io.Serializable

data class UserWalletId(val stringValue: String) : Serializable {

    val value = stringValue.hexToBytes()

    constructor(value: ByteArray?) : this(stringValue = value?.toHexString() ?: "")

    override fun toString() = "UserWalletId(${stringValue.take(n = 3)}...${stringValue.takeLast(n = 3)})"
}