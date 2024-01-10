package com.tangem.datasource.config.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExpressModel(
    @Json(name = "apiKey")
    val apiKey: String,
    @Json(name = "signVerifierPublicKey")
    val signVerifierPublicKey: String,
)