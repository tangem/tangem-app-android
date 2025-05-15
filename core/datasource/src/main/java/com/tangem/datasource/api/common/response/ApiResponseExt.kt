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