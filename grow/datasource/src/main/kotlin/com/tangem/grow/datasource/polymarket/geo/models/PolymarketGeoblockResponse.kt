@file:Suppress("BooleanPropertyNaming")

package com.tangem.grow.datasource.polymarket.geo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PolymarketGeoblockResponse(
    @SerialName("blocked") val blocked: Boolean,
)