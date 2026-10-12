package com.tangem.grow.datasource.collectibles.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for POST /api/v1/collectible/purchase-tx-sent */
@Serializable
data class PurchaseTxSentRequestBody(
    @SerialName("txId")
    val txId: String,
    @SerialName("txHash")
    val txHash: String,
)