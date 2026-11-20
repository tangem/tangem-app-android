package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The user's purchases that have not reached a final status yet, in the backend's order. */
@Serializable
data class ActiveTxsResponse(
    @SerialName("activeTxs")
    val activeTxs: List<CollectibleTxResponse>,
)