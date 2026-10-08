package com.tangem.grow.datasource.collectibles.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for POST /api/v1/collectible/open-pack */
@Serializable
data class CollectibleTxRequestBody(
    @SerialName("txId")
    val txId: String,
)