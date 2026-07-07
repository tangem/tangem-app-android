package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderRequest(
    @Json(name = "data") val data: Data,
    @Json(name = "idempotency_key") val idempotencyKey: String,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "customer_wallet_address") val customerWalletAddress: String,
        @Json(name = "specification_name") val specificationName: String = "SP_000004",
        @Json(name = "type") val type: String = "CARD_ISSUE_VIRTUAL_RAIN_KYC",
    )
}