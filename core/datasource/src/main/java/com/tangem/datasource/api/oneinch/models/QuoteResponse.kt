package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Quote response
 *
 * @property toToken Destination token info
 * @property toTokenAmount Expected amount of destination token
 */
data class QuoteResponse(
    @Json(name = "toToken") val toToken: TokenOneInchDto,
    @Json(name = "toAmount") val toTokenAmount: String,
)