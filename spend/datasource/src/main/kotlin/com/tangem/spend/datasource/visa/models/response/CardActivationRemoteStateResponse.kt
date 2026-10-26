package com.tangem.spend.datasource.visa.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardActivationRemoteStateResponse(
    @SerialName("result") val result: Result,
) {
    @Serializable
    data class Result(
        @SerialName("status") val status: String,
        @SerialName("order") val activationOrder: ActivationOrder?,
        @SerialName("step_error") val stepError: String?,
    )

    @Serializable
    data class ActivationOrder(
        @SerialName("id") val id: String,
        @SerialName("customer_id") val customerId: String,
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("card_wallet_address") val cardWalletAddress: String,
    )
}