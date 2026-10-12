package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetPinResponse(@SerialName("result") val result: Result?) {

    @Serializable
    data class Result(
        @SerialName("secret") val secret: String,
        @SerialName("iv") val iv: String,
    )
}