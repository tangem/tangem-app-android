package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KycAccessInfoResponse(
    @SerialName("result") val result: Result,
) {
    @Serializable
    data class Result(
        @SerialName("token") val token: String,
        @SerialName("locale") val locale: String,
    )
}