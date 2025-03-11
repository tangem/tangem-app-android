package com.tangem.data.common.api

import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.time.Duration

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
 * Attempts to execute an API call safely, providing error handling and a timeout.
 *
 * @param T The return type of the API call and the function.
 * @param timeoutMillis The timeout in milliseconds for the API call. Default is 30 seconds.
 * @param call The API call block to execute.
 * @param onError A function to handle errors and return a fallback value of type [T].
 *
 * @return The result of the API call or the fallback value provided by [onError] if an error occurs.
 */
suspend inline fun <T> safeApiCallWithTimeout(
    timeoutMillis: Duration = with(Duration) { 30.seconds },
    crossinline call: suspend ApiResponseRaise.() -> T,
    crossinline onError: suspend (ApiResponseError) -> T,
): T = safeApiCall(
    call = {
        withTimeoutOrNull(timeoutMillis) { call() }
            ?: raise(ApiResponseError.TimeoutException)
    },
    onError = onError,
)

/**
 * Attempts to execute an API call safely, providing error handling.
 *
 * @param T The return type of the API call and the function.
 * @param call The API call block to execute.
 * @param onError A function to handle errors and return a fallback value of type [T].
 *
 * @return The result of the API call or the fallback value provided by [onError] if an error occurs.
 */
suspend inline fun <T> safeApiCall(
    crossinline call: suspend ApiResponseRaise.() -> T,
    crossinline onError: suspend (ApiResponseError) -> T,
): T = recover(
    block = { call(ApiResponseRaise(raise = this)) },
    recover = {
        Timber.e(it, "Unable to perform safe API call")
        onError(it)
    },
)