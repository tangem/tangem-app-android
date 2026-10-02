package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `bff-v2/v1/geo/countries/{country_code}/regions`
 */
@Serializable
data class GeoRegionsResponse(
    @SerialName("result") val result: List<Region>,
) {

    @Serializable
    data class Region(
        @SerialName("code") val code: String,
        @SerialName("name") val name: String,
        @SerialName("suggested") val isSuggested: Boolean?,
    )
}