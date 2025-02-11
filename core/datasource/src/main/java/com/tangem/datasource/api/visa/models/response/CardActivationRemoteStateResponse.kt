package com.tangem.datasource.api.visa.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardActivationRemoteStateResponse(
    @Json(name = "activation_status") val status: String,
    @Json(name = "activation_order") val activationOrder: ActivationOrder?,
) {
    @JsonClass(generateAdapter = true)
    data class ActivationOrder(
        @Json(name = "id") val id: String,
        @Json(name = "customer_wallet_address") val customerWalletAddress: String,
    )
}