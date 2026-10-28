package com.tangem.grow.datasource.collectibles.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for POST /api/v1/buyback/buyback-tx-sent */
@Serializable
data class BuybackTxSentRequestBody(
    @SerialName("buybackId")
    val buybackId: String,
    @SerialName("txHash")
    val txHash: String,
)