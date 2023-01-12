package com.tangem.datasource.api.paymentology.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class CheckRegistrationRequests(
    @Json(name = "requests") val requests: List<Item>,
) {

    @JsonClass(generateAdapter = true)
    data class Item(
        @Json(name = "CID") val cardId: String = "",
        @Json(name = "publicKey") val publicKey: String = "",
    )
}
