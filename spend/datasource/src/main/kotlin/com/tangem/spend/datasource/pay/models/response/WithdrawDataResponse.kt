package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedRawJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WithdrawDataResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("hash") val hash: String,
        @SerialName("salt") val salt: String,
        @SerialName("sender_address") val senderAddress: String,
        @SerialName("structured_data") val structuredData: SerializedRawJson? = null,
    )
}