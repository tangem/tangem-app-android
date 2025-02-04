package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrenciesResponse(
    @Json(name = "currencies") val currencies: List<Currency>,
    @Json(name = "imageHost") val imageHost: String? = null,
) {

    @JsonClass(generateAdapter = true)
    data class Currency(
        @Json(name = "id") val id: String,
        @Json(name = "code") val code: String, // this is an uppercase id
        @Json(name = "name") val name: String,
        @Json(name = "rateBTC") val rateBTC: String,
        @Json(name = "unit") val unit: String, // $, €, ₽
        @Json(name = "type") val type: String,
        @Json(name = "iconSmallUrl") val iconSmallUrl: String? = null,
        @Json(name = "iconMediumUrl") val iconMediumUrl: String? = null,
    )
}