package com.tangem.datasource.api.common.response

/**
 * Represents the possible errors that can occur during an API request.
 */
sealed class ApiResponseError : Exception() {

    /**
     * Represents an HTTP exception, which typically occurs when the server responds
     * with a non-2xx HTTP status code.
     *
     * @property code The HTTP status code.
     * @property message A human-readable message describing the error.
     */
    data class HttpException(
        val code: Code,
        override val message: String?,
        val errorBody: String?,
    ) : ApiResponseError() {

        // region Error Codes
        enum class Code(val code: Int) {
            // 4xx Server Errors
            BAD_REQUEST(code = 400),
            UNAUTHORIZED(code = 401),
            PAYMENT_REQUIRED(code = 402),
            FORBIDDEN(code = 403),
            NOT_FOUND(code = 404),
            METHOD_NOT_ALLOWED(code = 405),
            NOT_ACCEPTABLE(code = 406),
            PROXY_AUTHENTICATION_REQUIRED(code = 407),
            REQUEST_TIMEOUT(code = 408),
            CONFLICT(code = 409),
            GONE(code = 410),
            LENGTH_REQUIRED(code = 411),
            PRECONDITION_FAILED(code = 412),
            PAYLOAD_TOO_LARGE(code = 413),
            URI_TOO_LONG(code = 414),
            UNSUPPORTED_MEDIA_TYPE(code = 415),
            RANGE_NOT_SATISFIABLE(code = 416),
            EXPECTATION_FAILED(code = 417),
            IM_A_TEAPOT(code = 418), // Not an error, but an April Fools' joke from RFC 2324
            UNPROCESSABLE_ENTITY(code = 422),
            LOCKED(code = 423),
            FAILED_DEPENDENCY(code = 424),
            TOO_EARLY(code = 425),
            UPGRADE_REQUIRED(code = 426),
            PRECONDITION_REQUIRED(code = 428),
            TOO_MANY_REQUESTS(code = 429),
            REQUEST_HEADER_FIELDS_TOO_LARGE(code = 431),
            UNAVAILABLE_FOR_LEGAL_REASONS(code = 451),
            // 5xx Server Errors
            INTERNAL_SERVER_ERROR(code = 500),
            NOT_IMPLEMENTED(code = 501),
            BAD_GATEWAY(code = 502),
            SERVICE_UNAVAILABLE(code = 503),
            GATEWAY_TIMEOUT(code = 504),
            HTTP_VERSION_NOT_SUPPORTED(code = 505),
            VARIANT_ALSO_NEGOTIATES(code = 506),
            INSUFFICIENT_STORAGE(code = 507),
            LOOP_DETECTED(code = 508),
            NOT_EXTENDED(code = 510),
            NETWORK_AUTHENTICATION_REQUIRED(code = 511),
            ;

            override fun toString(): String = "$code - $name"

            companion object {
                val values = values()
            }
        }
        // endregion Error Codes
    }

    /** Represents a network error, typically when there's no connectivity. */
    @Suppress("UnusedPrivateMember")
    data object NetworkException : ApiResponseError() {
        private fun readResolve(): Any = NetworkException
    }

    /** Represents a timeout error, typically when the server takes too long to respond. */
    @Suppress("UnusedPrivateMember")
    data object TimeoutException : ApiResponseError() {
        private fun readResolve(): Any = TimeoutException
    }

    /**
     * Represents an unexpected exception that doesn't fall into one of the other categories.
     *
     * @property cause The exception that caused this error.
     */
    data class UnknownException(override val cause: Throwable) : ApiResponseError()
}