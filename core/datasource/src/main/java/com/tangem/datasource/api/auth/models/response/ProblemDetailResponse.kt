package com.tangem.datasource.api.auth.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * RFC 9457 / RFC 7807 Problem Details response. Returned by Tangem Auth Service with
 * `Content-Type: application/problem+json` on every 4xx / 5xx response.
 */
@JsonClass(generateAdapter = true)
data class ProblemDetailResponse(
    /** URI identifying the problem type. */
    @Json(name = "type") val type: String,
    /** Short human-readable summary (e.g. `"Too Many Requests"`). */
    @Json(name = "title") val title: String,
    /** HTTP status code. */
    @Json(name = "status") val status: Int,
    /** Human-readable explanation. */
    @Json(name = "detail") val detail: String?,
    /** URI reference to this occurrence (e.g. `"/api/v1/auth/refresh"`). */
    @Json(name = "instance") val instance: String?,
    /** Application-specific error code. */
    @Json(name = "code") val code: String?,
    /** Retry delay for rate limiting (`429`). */
    @Json(name = "retryAfterSeconds") val retryAfterSeconds: Int?,
)