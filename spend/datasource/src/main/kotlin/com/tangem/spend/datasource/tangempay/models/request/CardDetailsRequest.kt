package com.tangem.spend.datasource.tangempay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardDetailsRequest(
    @SerialName("session_id") val sessionId: String,
)