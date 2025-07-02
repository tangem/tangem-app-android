package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardInfoBody(
    @Json(name = "card_id") val cardId: String,
    @Json(name = "card_public_key") val cardPublicKey: String,
)