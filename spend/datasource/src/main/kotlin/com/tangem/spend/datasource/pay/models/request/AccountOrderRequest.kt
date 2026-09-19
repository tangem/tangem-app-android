package com.tangem.spend.datasource.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AccountOrderRequest(
    @Json(name = "data") val data: Data,
    @Json(name = "idempotency_key") val idempotencyKey: String,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "deposit_address") val depositAddress: String,
        @Json(name = "type") val type: String,
        @Json(name = "specification_name") val specificationName: String,
        @Json(name = "signature") val signature: String?,
    )
}