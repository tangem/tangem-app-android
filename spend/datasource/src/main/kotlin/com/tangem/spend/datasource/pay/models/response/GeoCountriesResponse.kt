package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `bff-v2/v1/geo/countries`
 */
@Serializable
data class GeoCountriesResponse(
    @SerialName("result") val result: List<Country>,
) {

    @Serializable
    data class Country(
        @SerialName("code") val code: String,
        @SerialName("name") val name: String,
        @SerialName("flag_url") val flagUrl: String?,
        @SerialName("suggested") val isSuggested: Boolean?,
        @SerialName("has_regions") val hasRegions: Boolean?,
    )
}