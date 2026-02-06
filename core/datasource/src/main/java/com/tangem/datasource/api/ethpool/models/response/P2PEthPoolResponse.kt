package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Unified response wrapper for all P2P.org API responses
 *
 * All P2PEthPool API endpoints return responses in this format:
 * ```json
 * {
 *   "error": null | { code, message, name, errors },
 *   "result": { ... } | null
 * }
 * ```
 *
 * @param T The type of the result data
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolResponse<T>(
    @Json(name = "error")
    val error: P2PEthPoolErrorDetailsDTO?,
    @Json(name = "result")
    val result: T?,
)