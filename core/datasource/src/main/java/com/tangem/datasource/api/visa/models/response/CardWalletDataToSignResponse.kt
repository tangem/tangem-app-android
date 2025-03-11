package com.tangem.datasource.api.visa.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardWalletDataToSignResponse(
    @Json(name = "dataForCardWallet") val dataForCardWallet: Data,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "hash") val hash: String,
    )
}