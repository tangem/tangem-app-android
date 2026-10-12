package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetPinResponse(
    @SerialName("result") val result: Result?,
    @SerialName("error") val error: String?,
) {
    @Serializable
    data class Result(
        @SerialName("result") val result: String,
    )
}