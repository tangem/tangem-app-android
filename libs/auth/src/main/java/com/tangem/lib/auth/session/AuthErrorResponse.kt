package com.tangem.lib.auth.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * RFC 9457 / RFC 7807 Problem Details response. Returned by Tangem Auth Service with
 * `Content-Type: application/problem+json` on every 4xx / 5xx response.
 *
 * Every member is optional: RFC 9457 §3.1 defines them all as optional, and the auth service
 * omits several in practice (e.g. it never sends `type`). A required field here would make the
 * whole payload fail to deserialize when absent, collapsing `problem` to null and silently
 * disabling `detail`-based branching and `retryAfterSeconds` backoff — so keep them all nullable.
 */
@Serializable
data class AuthErrorResponse(
    /** URI identifying the problem type. */
    @SerialName("type") val type: String? = null,
    /** Short human-readable summary (e.g. `"Too Many Requests"`). */
    @SerialName("title") val title: String? = null,
    /** HTTP status code. */
    @SerialName("status") val status: Int? = null,
    /** Human-readable explanation. */
    @SerialName("detail") val detail: String? = null,
    /** URI reference to this occurrence (the request path that produced the error). */
    @SerialName("instance") val instance: String? = null,
    /** Application-specific error code. */
    @SerialName("code") val code: String? = null,
    /** Retry delay for rate limiting (`429`). */
    @SerialName("retryAfterSeconds") val retryAfterSeconds: Int? = null,
)