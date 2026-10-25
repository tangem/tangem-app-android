package com.tangem.grow.datasource.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketSessionSignersResponse(
    @Json(name = "wallet") val wallet: String? = null,
    @Json(name = "signers") val signers: List<PolymarketSessionSignerDto>? = null,
)

/**
 * @property validUntil Unix seconds; the CLOB sends it either as a number or as a decimal string, and Moshi's
 *  `Long` adapter reads both
 */
@JsonClass(generateAdapter = true)
data class PolymarketSessionSignerDto(
    @Json(name = "address") val address: String,
    @Json(name = "scopes") val scopes: List<String>? = null,
    @Json(name = "valid_until") val validUntil: Long? = null,
)