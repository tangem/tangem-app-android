package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayEligibilityChannels(
    @SerialName("result") val result: Result,
) {
    @Serializable
    data class Result(
        @SerialName("channels") val channels: List<String>,
    )
}