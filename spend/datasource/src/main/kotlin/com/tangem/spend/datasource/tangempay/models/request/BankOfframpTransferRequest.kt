package com.tangem.spend.datasource.tangempay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BankOfframpTransferRequest(
    @SerialName("quote_id") val quoteId: String,
)