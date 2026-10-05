package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeeResponse(
    @SerialName("result") val result: Result,
) {
    @Serializable
    data class Result(
        @SerialName("type") val type: String,
        @SerialName("amount") val amount: String,
        @SerialName("currency") val currency: String,
        @SerialName("description") val description: String,
    )
}