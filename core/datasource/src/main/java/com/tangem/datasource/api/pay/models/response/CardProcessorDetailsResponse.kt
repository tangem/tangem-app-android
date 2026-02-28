package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class CardProcessorDetailsResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "card_id") val cardId: String,
        @Json(name = "card_secret") val cardSecret: String,
    )
}