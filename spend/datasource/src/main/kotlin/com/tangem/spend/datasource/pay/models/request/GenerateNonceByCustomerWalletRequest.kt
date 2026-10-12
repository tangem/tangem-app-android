package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateNonceByCustomerWalletRequest(
    @SerialName("auth_type") val authType: String = "customer_wallet",
    @SerialName("customer_wallet_address") val customerWalletAddress: String,
    @SerialName("customer_wallet_id") val customerWalletId: String,
)