package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `bff-v2/v1/geo/countries`
 */
@JsonClass(generateAdapter = true)
data class GeoCountriesResponse(
    @Json(name = "result") val result: List<Country>,
) {

    @JsonClass(generateAdapter = true)
    data class Country(
        @Json(name = "code") val code: String,
        @Json(name = "name") val name: String,
        @Json(name = "flag_url") val flagUrl: String?,
        @Json(name = "suggested") val isSuggested: Boolean?,
        @Json(name = "has_regions") val hasRegions: Boolean?,
    )
}