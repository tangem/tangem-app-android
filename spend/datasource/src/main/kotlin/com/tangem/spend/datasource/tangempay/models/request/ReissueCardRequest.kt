package com.tangem.spend.datasource.tangempay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReissueCardRequest(
    @SerialName("card_id") val cardId: String,
)