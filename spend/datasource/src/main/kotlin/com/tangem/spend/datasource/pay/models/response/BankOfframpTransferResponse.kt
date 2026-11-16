package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BankOfframpTransferResponse(
    @SerialName("result") val result: Result?,
) {

    @Serializable
    data class Result(
        @SerialName("transfer_id") val transferId: String?,
        @SerialName("deposit_address") val depositAddress: String?,
        @SerialName("source_amount") val sourceAmount: SerializedBigDecimal?,
        @SerialName("destination_amount") val destinationAmount: SerializedBigDecimal?,
        @SerialName("expires_at") val expiresAt: String?,
    )
}