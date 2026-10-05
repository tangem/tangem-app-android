package com.tangem.spend.datasource.visa.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisaDataToSignResponse(
    @SerialName("result") val result: Result,
) {
    @Serializable
    data class Result(
        @SerialName("hash") val hash: String,
    )
}