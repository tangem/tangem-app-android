package com.tangem.spend.datasource.visa.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateNoneByCardWalletRequest(
    @SerialName("auth_type") val authType: String = "card_wallet",
    @SerialName("card_wallet_address") val cardWalletAddress: String,
    @SerialName("card_id") val cardId: String,
)