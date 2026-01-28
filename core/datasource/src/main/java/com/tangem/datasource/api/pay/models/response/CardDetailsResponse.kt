package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class CardDetailsResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "token") val token: String,
        @Json(name = "expiration_month") val expirationMonth: String,
        @Json(name = "expiration_year") val expirationYear: String,
        @Json(name = "emboss_name") val embossName: String,
        @Json(name = "card_type") val cardType: String,
        @Json(name = "card_status") val cardStatus: String,
        @Json(name = "card_number_end") val cardNumberEnd: String,
        @Json(name = "pan") val pan: Secret,
        @Json(name = "cvv") val cvv: Secret,
    )

    @JsonClass(generateAdapter = true)
    data class Secret(
        @Json(name = "secret") val secret: String,
        @Json(name = "iv") val iv: String,
    )
}