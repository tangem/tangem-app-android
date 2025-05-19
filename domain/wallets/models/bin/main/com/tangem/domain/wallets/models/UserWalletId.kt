package com.tangem.domain.wallets.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class UserWalletId(
    @Json(name = "stringValue")
    val stringValue: String,
) {

    val value = stringValue.hexToBytes()

    constructor(value: ByteArray?) : this(stringValue = value?.toHexString() ?: "")

    override fun toString() = "UserWalletId(${stringValue.take(n = 3)}...${stringValue.takeLast(n = 3)})"
}