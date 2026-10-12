package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `bff-v2/v1/geo/countries/{country_code}/regions`
 */
@JsonClass(generateAdapter = true)
data class GeoRegionsResponse(
    @Json(name = "result") val result: List<Region>,
) {

    @JsonClass(generateAdapter = true)
    data class Region(
        @Json(name = "code") val code: String,
        @Json(name = "name") val name: String,
        @Json(name = "suggested") val isSuggested: Boolean?,
    )
}