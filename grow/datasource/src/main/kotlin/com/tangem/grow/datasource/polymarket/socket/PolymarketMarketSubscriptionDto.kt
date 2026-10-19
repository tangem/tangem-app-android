package com.tangem.grow.datasource.polymarket.socket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PolymarketMarketSubscriptionDto(
    @SerialName("assets_ids") val assetIds: List<String>,
) {

    @SerialName("type")
    val type: String = "market"

    @SerialName("custom_feature_enabled")
    val isCustomFeatureEnabled: Boolean = true
}

@Serializable
data class PolymarketMarketSubscriptionUpdateDto(
    @SerialName("assets_ids") val assetIds: List<String>,
    @SerialName("operation") val operation: Operation,
) {

    @Serializable
    enum class Operation {
        @SerialName("subscribe")
        SUBSCRIBE,

        @SerialName("unsubscribe")
        UNSUBSCRIBE,
    }
}