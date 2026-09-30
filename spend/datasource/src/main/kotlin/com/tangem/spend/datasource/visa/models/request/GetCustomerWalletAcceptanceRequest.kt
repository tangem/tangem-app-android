package com.tangem.spend.datasource.visa.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetCustomerWalletAcceptanceRequest(
    @SerialName("type") val type: String = "customer_wallet",
    @SerialName("customer_wallet_address") val customerWalletAddress: String,
    @SerialName("card_wallet_address") val cardWalletAddress: String,
)