package com.tangem.domain.walletconnect.model.legacy

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Account(
    @Json(name = "chainId")
    val chainId: String,

    @Json(name = "walletAddress")
    val walletAddress: String,

    @Json(name = "derivationPath")
    val derivationPath: String?,
)