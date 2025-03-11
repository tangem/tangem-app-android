package com.tangem.datasource.local.config.issuers.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Issuer(
    @Json(name = "privateKey") val privateKey: String,
    @Json(name = "publicKey") val publicKey: String,
)