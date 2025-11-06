package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class FreezeUnfreezeCardResponse(
    @Json(name = "result") val result: Result?,
    @Json(name = "error") val error: String?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "order_id") val orderId: String,
        @Json(name = "status") val status: Status,
    )

    @JsonClass(generateAdapter = false)
    enum class Status {
        @Json(name = "NEW")
        NEW,

        @Json(name = "PROCESSING")
        PROCESSING,

        @Json(name = "COMPLETED")
        COMPLETED,

        @Json(name = "CANCELED")
        CANCELED,
    }
}