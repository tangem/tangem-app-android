package com.tangem.spend.datasource.visa.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivationByCustomerWalletRequest(
    @SerialName("order_id") val orderId: String,
    @SerialName("customer_wallet") val customerWallet: CustomerWallet,
) {
    @Serializable
    data class CustomerWallet(
        @SerialName("deploy_acceptance_signature") val deployAcceptanceSignature: String,
        @SerialName("address") val customerWalletAddress: String,
    )
}