package com.tangem.lib.auth.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * RFC 9457 / RFC 7807 Problem Details response. Returned by Tangem Auth Service with
 * `Content-Type: application/problem+json` on every 4xx / 5xx response.
 */
@Serializable
data class AuthErrorResponse(
    /** URI identifying the problem type. */
    @SerialName("type") val type: String,
    /** Short human-readable summary (e.g. `"Too Many Requests"`). */
    @SerialName("title") val title: String,
    /** HTTP status code. */
    @SerialName("status") val status: Int,
    /** Human-readable explanation. */
    @SerialName("detail") val detail: String? = null,
    /** URI reference to this occurrence (e.g. `"/api/v1/auth/refresh"`). */
    @SerialName("instance") val instance: String? = null,
    /** Application-specific error code. */
    @SerialName("code") val code: String? = null,
    /** Retry delay for rate limiting (`429`). */
    @SerialName("retryAfterSeconds") val retryAfterSeconds: Int? = null,
)