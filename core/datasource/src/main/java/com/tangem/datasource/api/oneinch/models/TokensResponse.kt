package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Tokens response
 *
 * @property tokens List of supported tokens
 */
data class TokensResponse(
    @Json(name = "tokens") val tokens: Map<String, TokenOneInchDto>,
)
