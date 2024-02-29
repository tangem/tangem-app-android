package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateUserNetworkAccountBody(
    @Json(name = "networkId") val networkId: String,
    @Json(name = "walletPublicKey") val walletPublicKey: String,
)