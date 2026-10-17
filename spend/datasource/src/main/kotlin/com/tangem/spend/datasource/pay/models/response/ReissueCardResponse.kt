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
        /**
         * A status this build does not know falls back to [OrderResponse.Result.Status.UNKNOWN] since app version 6.4.
         * Older versions fail to parse the whole response on it.
         */
        @SerialName("status") val status: OrderResponse.Result.Status = OrderResponse.Result.Status.UNKNOWN,
    )
}