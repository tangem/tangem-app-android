package com.tangem.datasource.api.visa.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActivationStatusRequest(
    @Json(name = "card_id") val cardId: String,
    @Json(name = "card_public_key") val cardPublicKey: String,
)