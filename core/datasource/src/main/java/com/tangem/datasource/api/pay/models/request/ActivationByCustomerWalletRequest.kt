package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActivationByCustomerWalletRequest(
    @Json(name = "order_id") val orderId: String,
    @Json(name = "customer_wallet") val customerWallet: CustomerWallet,
) {
    @JsonClass(generateAdapter = true)
    data class CustomerWallet(
        @Json(name = "deploy_acceptance_signature") val deployAcceptanceSignature: String,
        @Json(name = "address") val customerWalletAddress: String,
    )
}