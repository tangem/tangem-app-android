package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Error response structure for P2P.org API
 *
 * All P2P API endpoints return errors in this format
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolErrorResponse(
    @Json(name = "error")
    val error: P2PEthPoolErrorDetailsDTO,
    @Json(name = "result")
    val result: Any? = null, // null on error
)

@JsonClass(generateAdapter = true)
data class P2PEthPoolErrorDetailsDTO(
    @Json(name = "code")
    val code: Int, // Error code (e.g., 127106, 101111)
    @Json(name = "message")
    val message: String, // Human-readable error message
    @Json(name = "name")
    val name: String, // Error name/type
    @Json(name = "errors")
    val errors: List<String>? = null, // Optional validation errors array
)