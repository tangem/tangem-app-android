package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class FeesResponse(
    @Json(name = "result") val result: List<Fee>?,
) {
    @JsonClass(generateAdapter = true)
    data class Fee(
        @Json(name = "type") val type: String?,
        @Json(name = "group") val group: String?,
        @Json(name = "name") val name: String?,
        @Json(name = "calculation_type") val calculationType: String?,
        @Json(name = "amount") val amount: BigDecimal?,
        @Json(name = "currency") val currency: String?,
        @Json(name = "description") val description: String?,
    )
}