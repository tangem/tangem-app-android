package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReissueCardResponse(
    @SerialName("result") val result: Result,
) {
    @Serializable
    data class Result(
        @SerialName("order_id") val orderId: String,
        @SerialName("status") val status: OrderResponse.Result.Status = OrderResponse.Result.Status.UNKNOWN,
    )
}