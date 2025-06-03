package com.tangem.datasource.api.visa.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardActivationRemoteStateResponse(
    @Json(name = "result") val result: Result,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "status") val status: String,
        @Json(name = "order") val activationOrder: ActivationOrder?,
        @Json(name = "stepChangeCode") val stepChangeCode: Int?,
        @Json(name = "updatedAt") val updatedAt: String?,
    )

    @JsonClass(generateAdapter = true)
    data class ActivationOrder(
        @Json(name = "id") val id: String,
        @Json(name = "customer_id") val customerId: String,
        @Json(name = "customer_wallet_address") val customerWalletAddress: String,
        @Json(name = "card_wallet_address") val cardWalletAddress: String,
    )
}