package com.tangem.datasource.api.common.response

fun <T : Any> ApiResponse<T>.getOrThrow(): T = when (this) {
    is ApiResponse.Error -> throw cause
    is ApiResponse.Success -> data
}

fun <T : Any, R : Any> ApiResponse<T>.fold(onSuccess: (T) -> R, onError: (ApiResponseError) -> R): R {
    return when (this) {
        is ApiResponse.Error -> onError(cause)
        is ApiResponse.Success -> onSuccess(data)
    }
}

inline fun <T> catchApiResponseError(onError: (ApiResponseError) -> Unit, block: () -> T): T {
    return try {
        block()
    } catch (e: ApiResponseError) {
        onError(e)
        throw e
    }
}

/**
 * Checks if the [ApiResponseError] is a network-related error.
 * You can provide a custom predicate [codePredicate] to check for specific HTTP status codes.
 */
inline fun ApiResponseError.isNetworkError(
    codePredicate: (ApiResponseError.HttpException.Code) -> Boolean = { true },
): Boolean {
    return this is ApiResponseError.HttpException && codePredicate(this.code)
}