package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCardRequest(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("card_limit") val cardLimit: CardLimit? = null,
) {
    @Serializable
    data class CardLimit(
        @SerialName("amount") val amount: String,
    )
}