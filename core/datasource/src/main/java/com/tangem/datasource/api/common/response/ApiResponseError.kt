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
        enum class Code(val numericCode: Int) {
            // 3xx Server Errors
            NOT_MODIFIED(numericCode = 304),
            // 4xx Server Errors
            BAD_REQUEST(numericCode = 400),
            UNAUTHORIZED(numericCode = 401),
            PAYMENT_REQUIRED(numericCode = 402),
            FORBIDDEN(numericCode = 403),
            NOT_FOUND(numericCode = 404),
            METHOD_NOT_ALLOWED(numericCode = 405),
            NOT_ACCEPTABLE(numericCode = 406),
            PROXY_AUTHENTICATION_REQUIRED(numericCode = 407),
            REQUEST_TIMEOUT(numericCode = 408),
            CONFLICT(numericCode = 409),
            GONE(numericCode = 410),
            LENGTH_REQUIRED(numericCode = 411),
            PRECONDITION_FAILED(numericCode = 412),
            PAYLOAD_TOO_LARGE(numericCode = 413),
            URI_TOO_LONG(numericCode = 414),
            UNSUPPORTED_MEDIA_TYPE(numericCode = 415),
            RANGE_NOT_SATISFIABLE(numericCode = 416),
            EXPECTATION_FAILED(numericCode = 417),
            IM_A_TEAPOT(numericCode = 418), // Not an error, but an April Fools' joke from RFC 2324
            UNPROCESSABLE_ENTITY(numericCode = 422),
            LOCKED(numericCode = 423),
            FAILED_DEPENDENCY(numericCode = 424),
            TOO_EARLY(numericCode = 425),
            UPGRADE_REQUIRED(numericCode = 426),
            PRECONDITION_REQUIRED(numericCode = 428),
            TOO_MANY_REQUESTS(numericCode = 429),
            REQUEST_HEADER_FIELDS_TOO_LARGE(numericCode = 431),
            UNAVAILABLE_FOR_LEGAL_REASONS(numericCode = 451),
            // 5xx Server Errors
            INTERNAL_SERVER_ERROR(numericCode = 500),
            NOT_IMPLEMENTED(numericCode = 501),
            BAD_GATEWAY(numericCode = 502),
            SERVICE_UNAVAILABLE(numericCode = 503),
            GATEWAY_TIMEOUT(numericCode = 504),
            HTTP_VERSION_NOT_SUPPORTED(numericCode = 505),
            VARIANT_ALSO_NEGOTIATES(numericCode = 506),
            INSUFFICIENT_STORAGE(numericCode = 507),
            LOOP_DETECTED(numericCode = 508),
            NOT_EXTENDED(numericCode = 510),
            NETWORK_AUTHENTICATION_REQUIRED(numericCode = 511),
            ;

            override fun toString(): String = "$numericCode - $name"
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