package com.tangem.datasource.api.common.response

/**
 * Represents the possible responses from an API request.
 *
 * @param T The type of the data that is expected in a successful response.
 */
sealed class ApiResponse<T : Any> {

    /**
     * Represents a successful response from the API.
     *
     * @property data The data returned by the API.
     */
    data class Success<T : Any>(val data: T) : ApiResponse<T>()

    /**
     * Represents an error response or failure from the API.
     *
     * @property cause The cause of the error.
     */
    data class Error(val cause: ApiResponseError) : ApiResponse<Nothing>()
}

/**
 * Wraps data in a [ApiResponse.Success] instance.
 *
 * @param data The data to wrap.
 * @return A [ApiResponse.Success] instance containing the provided data.
 */
internal fun <T : Any> apiSuccess(data: T): ApiResponse<T> = ApiResponse.Success(data)

/**
 * Wraps an [ApiResponseError] in a [ApiResponse.Error] instance.
 *
 * @param cause The error to wrap.
 * @return A [ApiResponse.Error] instance containing the provided error.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> apiError(cause: ApiResponseError): ApiResponse<T> = ApiResponse.Error(cause) as ApiResponse<T>