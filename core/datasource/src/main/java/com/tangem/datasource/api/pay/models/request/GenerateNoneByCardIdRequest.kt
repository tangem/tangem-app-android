package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateNoneByCardIdRequest(
    @Json(name = "auth_type") val authType: String = "card_id",
    @Json(name = "card_id") val cardId: String,
    @Json(name = "card_public_key") val cardPublicKey: String,
)