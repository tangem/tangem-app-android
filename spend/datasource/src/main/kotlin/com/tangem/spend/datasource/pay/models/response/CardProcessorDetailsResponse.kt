package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardProcessorDetailsResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("card_id") val cardId: String,
        @SerialName("card_secret") val cardSecret: String?,
    ) {
        override fun toString(): String = "Result(cardId=$cardId, cardSecret=***)"
    }
}