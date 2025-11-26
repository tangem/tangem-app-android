package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WalletBody(
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: WalletType? = null,
)