package com.tangem.datasource.api.common.response

import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError

/**
 * Wraps data in a [ApiResponse.Success] instance
 *
 * @param data    the data to wrap
 * @param code    the HTTP status code of the response
 * @param headers the headers returned by the API
 * @return a [ApiResponse.Success] instance containing the provided data
 */
internal fun <T : Any> apiSuccess(
    data: T,
    code: ApiResponseError.HttpException.Code?,
    headers: Map<String, List<String>>,
): ApiResponse<T> {
    return ApiResponse.Success(
        data = data,
        code = code ?: ApiResponseError.HttpException.Code.OK,
        headers = headers,
    )
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