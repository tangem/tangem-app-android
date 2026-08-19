package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response from `GET /v1/customer/cashback/history`
 */
@JsonClass(generateAdapter = true)
data class CashbackHistoryResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "items") val items: List<Item>?,
    )

    @JsonClass(generateAdapter = true)
    data class Item(
        @Json(name = "year") val year: Int,
        @Json(name = "month") val month: Int,
        @Json(name = "confirmed_amount") val confirmedAmount: BigDecimal,
        @Json(name = "currency") val currency: String,
    )
}