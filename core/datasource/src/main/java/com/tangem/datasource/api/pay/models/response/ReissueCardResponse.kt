package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReissueCardResponse(
    @Json(name = "result") val result: Result,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "order_id") val orderId: String,
        @Json(name = "status") val status: OrderResponse.Result.Status,
    )
}