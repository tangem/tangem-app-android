package com.tangem.domain.walletconnect.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WcEthSignTypedDataParams(
    @Json(name = "message")
    val message: String?,
)