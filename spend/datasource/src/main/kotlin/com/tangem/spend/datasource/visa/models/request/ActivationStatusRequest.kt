package com.tangem.spend.datasource.visa.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivationStatusRequest(
    @SerialName("card_id") val cardId: String,
    @SerialName("card_public_key") val cardPublicKey: String,
)