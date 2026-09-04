package com.tangem.domain.transaction.error

/**
 * Failure of the gasless backend.
 *
 * Carries [httpCode] so the send flow can show the status to the user and attach it to a support request:
 * the transport exception it replaces loses that (`Response.message()` is empty over HTTP/2, and the
 * error type is not visible from the domain layer).
 *
 * @property httpCode HTTP status of the failed call, `null` when the call itself succeeded but the service
 * reported an unsuccessful result.
 */
class GaslessServiceException(
    val httpCode: Int?,
    override val message: String?,
) : Exception()