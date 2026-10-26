package com.tangem.spend.datasource.visa.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateNoneByCardIdRequest(
    @SerialName("auth_type") val authType: String = "card_id",
    @SerialName("card_id") val cardId: String,
    @SerialName("card_public_key") val cardPublicKey: String,
)