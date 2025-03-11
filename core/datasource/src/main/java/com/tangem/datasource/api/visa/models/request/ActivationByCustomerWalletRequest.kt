package com.tangem.datasource.api.visa.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActivationByCustomerWalletRequest(
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "product_instance_id") val productInstanceId: String,
    @Json(name = "activation_order_id") val activationOrderId: String,
    @Json(name = "data") val data: Data,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "customer_wallet") val customerWallet: CustomerWallet,
    )

    @JsonClass(generateAdapter = true)
    data class CustomerWallet(
        @Json(name = "address") val address: String,
        @Json(name = "deploy_acceptance_signature") val deployAcceptanceSignature: String,
    )
}