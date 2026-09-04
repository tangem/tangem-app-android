package com.tangem.domain.transaction.error

/**
 * Failure of the gasless backend.
 *
 * Carries [httpCode] so the send flow can show the status to the user and attach it to a support request:
 * the transport exception it replaces loses that (`Response.message()` is empty over HTTP/2, and the
 * error type is not visible from the domain layer).
 *
 * @property httpCode HTTP status of the failed call, `null` when there is none to report — the service
 * answered with an unsuccessful result, the request never got a response (no connectivity, timeout), or
 * the status was not recognised.
 */
class GaslessServiceException(
    val httpCode: Int?,
    message: String?,
    cause: Throwable? = null,
) : Exception(message, cause)