package com.tangem.core.remote.response

/**
 * Represents the possible responses from an API request.
 *
 * @param T The type of the data that is expected in a successful response.
 */
sealed class ApiResponse<T : Any> {

    /** Map of headers (header name with list of values) */
    abstract val headers: Map<String, List<String>>

    /**
     * Represents a successful response from the API
     *
     * @property data    the data returned by the API
     * @property code    the HTTP status code of the response
     * @property headers the headers returned by the API
     */
    data class Success<T : Any>(
        val data: T,
        val code: ApiResponseError.HttpException.Code = ApiResponseError.HttpException.Code.OK,
        override val headers: Map<String, List<String>> = emptyMap(),
    ) : ApiResponse<T>()

    /**
     * Represents an error response or failure from the API
     *
     * @property cause   the cause of the error
     * @property headers the headers returned by the API
     */
    data class Error(
        val cause: ApiResponseError,
        override val headers: Map<String, List<String>> = emptyMap(),
    ) : ApiResponse<Nothing>()
}