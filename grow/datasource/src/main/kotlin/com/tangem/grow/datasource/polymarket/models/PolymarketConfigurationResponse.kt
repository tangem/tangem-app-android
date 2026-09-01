package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET /api/predictions/v1/configuration`.
 *
 * @property builderCode Tangem's identifier at Polymarket, `0x` + 64 hex, sent back unchanged as the
 *  `X-Builder-Code` header of the bridge deposit and withdraw calls
 */
@JsonClass(generateAdapter = true)
data class PolymarketConfigurationResponse(
    @Json(name = "builderCode") val builderCode: String,
)