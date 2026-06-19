package com.tangem.lib.auth.session

/**
 * Typed Tangem Auth Service failure produced by `AuthErrorConverter` out of the raw
 * `ApiResponseError`. Carries the parsed [AuthErrorResponse] when the server returned
 * an `application/problem+json` body (RFC 9457).
 */
sealed class AuthError(open val problem: AuthErrorResponse?) {

    /** `400` — invalid nonce / signature / wallet already registered. */
    data class BadRequest(override val problem: AuthErrorResponse?) : AuthError(problem)

    /** `401` — invalid / expired / revoked / replayed access or refresh token. */
    data class Unauthorized(override val problem: AuthErrorResponse?) : AuthError(problem)

    /** `403` — device blocked (RED tier), max wallets exceeded, or token-device mismatch. */
    data class Forbidden(override val problem: AuthErrorResponse?) : AuthError(problem)

    /** `404` — token / resource not found. */
    data class NotFound(override val problem: AuthErrorResponse?) : AuthError(problem)

    /** `409` — conflict / already exists (e.g. device or wallet already registered). */
    data class Conflict(override val problem: AuthErrorResponse?) : AuthError(problem)

    /** `429` — server-side rate limit; honour [retryAfterSeconds] before retrying. */
    data class RateLimited(
        val retryAfterSeconds: Int?,
        override val problem: AuthErrorResponse?,
    ) : AuthError(problem)

    /** `5xx` — server-side outage. */
    data class ServerUnavailable(override val problem: AuthErrorResponse?) : AuthError(problem)

    /** Connectivity issue (DNS, timeout, offline). */
    data object NetworkError : AuthError(problem = null)

    /** Anything not covered above (parsing failure, unexpected exception). */
    data class Unknown(val cause: Throwable) : AuthError(problem = null)
}