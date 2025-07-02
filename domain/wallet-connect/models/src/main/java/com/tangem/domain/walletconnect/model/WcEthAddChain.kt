package com.tangem.domain.walletconnect.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WcEthAddChain(
    @Json(name = "chainId")
    val chainId: String,
)