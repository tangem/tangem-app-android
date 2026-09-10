package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImagesResponse(
    @Json(name = "result") val result: List<Image>,
) {

    @JsonClass(generateAdapter = true)
    data class Image(
        @Json(name = "type") val type: String?,
        @Json(name = "url") val url: String?,
    )
}