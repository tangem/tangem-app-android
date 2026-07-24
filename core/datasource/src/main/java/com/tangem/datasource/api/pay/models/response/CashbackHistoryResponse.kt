package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response from `GET /v1/customer/cashback/history`.
 *
 * Confirmed cashback grouped by calendar month, ordered oldest to newest. Drives the monthly
 * earnings histogram on the Cashback screen.
 */
@JsonClass(generateAdapter = true)
data class CashbackHistoryResponse(
    @Json(name = "currency") val currency: String?,
    @Json(name = "items") val items: List<Item>?,
) {

    @JsonClass(generateAdapter = true)
    data class Item(
        @Json(name = "year") val year: Int,
        @Json(name = "month") val month: Int,
        @Json(name = "confirmed_amount") val confirmedAmount: BigDecimal?,
    )
}