package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketTeamDto(
    @Json(name = "name") val name: String,
    @Json(name = "abbreviation") val abbreviation: String? = null,
    @Json(name = "alias") val alias: String? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "ordering") val ordering: String? = null,
)