package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Quote response
 *
 * @property fromToken Source token info
 * @property toToken Destination token info
 * @property toTokenAmount Expected amount of destination token
 * @property fromTokenAmount Amount of source token
 * @property estimatedGas gas fee
 */
data class QuoteResponse(
    @Json(name = "fromToken") val fromToken: TokenOneInchDto,
    @Json(name = "toToken") val toToken: TokenOneInchDto,
    @Json(name = "toTokenAmount") val toTokenAmount: String,
    @Json(name = "fromTokenAmount") val fromTokenAmount: String,
    @Json(name = "estimatedGas") val estimatedGas: Int,
)
