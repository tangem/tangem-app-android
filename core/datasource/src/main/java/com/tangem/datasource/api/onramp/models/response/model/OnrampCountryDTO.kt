package com.tangem.datasource.api.onramp.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnrampCountryDTO(
    @Json(name = "name")
    val name: String,

    @Json(name = "code")
    val code: String,

    @Json(name = "image")
    val image: String,

    @Json(name = "alpha3")
    val alpha3: String,

    @Json(name = "continent")
    val continent: String,

    @Json(name = "defaultCurrency")
    val defaultCurrency: OnrampCurrencyDTO,

    @Json(name = "onrampAvailable")
    val onrampAvailable: Boolean,
)