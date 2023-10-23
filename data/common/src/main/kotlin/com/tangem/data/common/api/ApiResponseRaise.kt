package com.tangem.data.common.api

import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import timber.log.Timber

/**
 * A wrapper around the [Raise] interface specific for [ApiResponseError]. It provides utility functions to
 * operate on [ApiResponse] instances.
 *
 * @property raise A [Raise] instance for raising [ApiResponseError].
 */
@JvmInline
value class ApiResponseRaise(
    private val raise: Raise<ApiResponseError>,
) : Raise<ApiResponseError> by raise {

    /**
     * Binds the given [ApiResponse] to its underlying value or raises an error.
     *
     * @return The underlying data of the response if it's successful.
     */
    fun <T : Any> ApiResponse<T>.bind(): T = when (this) {
        is ApiResponse.Success -> data
        is ApiResponse.Error -> raise.raise(cause)
    }
}

/**
 * Attempts to execute an API call safely, providing error handling.
 *
 * @param call The API call block to execute.
 * @param onError A function to handle errors and return a fallback value of type [T].
 *
 * @return The result of the API call or the fallback value provided by [onError] if an error occurs.
 */
inline fun <T> safeApiCall(call: ApiResponseRaise.() -> T, onError: (ApiResponseError) -> T): T {
    return recover(
        block = { call(ApiResponseRaise(raise = this)) },
        recover = {
            Timber.w(it, "Unable to perform safe API call")
            onError(it)
        },
    )
}
