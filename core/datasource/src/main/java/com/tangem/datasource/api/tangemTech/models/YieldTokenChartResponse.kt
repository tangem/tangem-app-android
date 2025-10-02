package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldTokenChartResponse(
    @Json(name = "underlying") val underlying: String,
    @Json(name = "market") val market: String,
    @Json(name = "bucketSizeDays") val bucketSizeDays: Int,
    @Json(name = "period") val period: String,
    @Json(name = "data") val data: List<DataPoint>,
    @Json(name = "avr") val averageApy: BigDecimal,
) {

    @JsonClass(generateAdapter = true)
    data class DataPoint(
        @Json(name = "bucketIndex") val bucketIndex: Int,
        @Json(name = "avgApy") val avgApy: BigDecimal,
    )
}