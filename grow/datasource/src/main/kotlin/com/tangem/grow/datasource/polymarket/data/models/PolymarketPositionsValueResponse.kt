package com.tangem.grow.datasource.polymarket.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class PolymarketPositionsValueResponse(
    @Json(name = "data") val data: Data,
) {

    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "value") val value: BigDecimal,
    )
}