package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateCardRequest(
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "card_limit") val cardLimit: CardLimit? = null,
) {
    @JsonClass(generateAdapter = true)
    data class CardLimit(
        @Json(name = "amount") val amount: String,
    )
}