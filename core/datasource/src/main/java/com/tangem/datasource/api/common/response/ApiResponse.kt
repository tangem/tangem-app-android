package com.tangem.datasource.api.common.response

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
     * @property headers the headers returned by the API
     */
    data class Success<T : Any>(
        val data: T,
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

/**
 * Wraps data in a [ApiResponse.Success] instance
 *
 * @param data    the data to wrap
 * @param headers the headers returned by the API
 * @return a [ApiResponse.Success] instance containing the provided data
 */
internal fun <T : Any> apiSuccess(data: T, headers: Map<String, List<String>>): ApiResponse<T> {
    return ApiResponse.Success(data, headers)
}

/**
 * Wraps an [ApiResponseError] in a [ApiResponse.Error] instance
 *
 * @param cause   the error to wrap
 * @param headers the headers returned by the API
 * @return a [ApiResponse.Error] instance containing the provided error
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> apiError(cause: ApiResponseError, headers: Map<String, List<String>>): ApiResponse<T> {
    return ApiResponse.Error(cause, headers) as ApiResponse<T>
}