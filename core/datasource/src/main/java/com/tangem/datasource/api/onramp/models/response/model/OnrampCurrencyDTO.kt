package com.tangem.datasource.api.onramp.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnrampCurrencyDTO(
    @Json(name = "name")
    val name: String,

    @Json(name = "code")
    val code: String,

    @Json(name = "image")
    val image: String?,

    @Json(name = "precision")
    val precision: Int,

    @Json(name = "unit")
    val unit: String?,
)