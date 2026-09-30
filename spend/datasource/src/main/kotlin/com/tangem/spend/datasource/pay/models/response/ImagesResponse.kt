package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImagesResponse(
    @SerialName("result") val result: List<Image>,
) {

    @Serializable
    data class Image(
        @SerialName("type") val type: String?,
        @SerialName("url") val url: String?,
    )
}