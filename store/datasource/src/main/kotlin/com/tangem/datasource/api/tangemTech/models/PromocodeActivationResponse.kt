package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PromocodeActivationResponse(
    @Json(name = "status") val status: String,
    @Json(name = "promoProgram") val promoProgram: PromoProgram? = null,
) {

    @JsonClass(generateAdapter = true)
    data class PromoProgram(
        @Json(name = "id") val id: String,
    )
}