package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardDetailsResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("token") val token: String,
        @SerialName("expiration_month") val expirationMonth: String,
        @SerialName("expiration_year") val expirationYear: String,
        @SerialName("emboss_name") val embossName: String?,
        @SerialName("card_type") val cardType: String,
        @SerialName("card_status") val cardStatus: String,
        @SerialName("card_number_end") val cardNumberEnd: String,
        @SerialName("pan") val pan: Secret,
        @SerialName("cvv") val cvv: Secret,
    )

    @Serializable
    data class Secret(
        @SerialName("secret") val secret: String,
        @SerialName("iv") val iv: String,
    )
}