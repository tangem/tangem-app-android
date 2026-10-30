package com.tangem.data.polymarket.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** @property validUntil Unix seconds of the authorization Polymarket listed the key with */
@Serializable
internal data class PolymarketSessionKeyReadinessDTO(
    @SerialName("address") val address: String,
    @SerialName("valid_until") val validUntil: Long,
)