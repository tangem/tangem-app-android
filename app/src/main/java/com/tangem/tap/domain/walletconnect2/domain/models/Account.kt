package com.tangem.tap.domain.walletconnect2.domain.models

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