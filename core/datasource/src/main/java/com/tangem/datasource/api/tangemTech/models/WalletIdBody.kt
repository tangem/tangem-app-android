package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WalletIdBody(
    @Json(name = "id") val walletId: String,
    @Json(name = "name") val name: String,
    @Json(name = "cards") val cards: List<CardInfoBody>,
)