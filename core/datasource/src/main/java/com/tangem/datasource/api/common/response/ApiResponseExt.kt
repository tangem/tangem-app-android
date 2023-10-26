package com.tangem.datasource.api.common.response

fun <T : Any> ApiResponse<T>.getOrThrow(): T = when (this) {
    is ApiResponse.Error -> throw cause
    is ApiResponse.Success -> data
}