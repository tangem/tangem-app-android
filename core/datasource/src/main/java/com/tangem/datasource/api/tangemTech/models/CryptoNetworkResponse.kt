package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CryptoNetworkResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "networkId") val networkId: String,
    @Json(name = "name") val name: String,
)