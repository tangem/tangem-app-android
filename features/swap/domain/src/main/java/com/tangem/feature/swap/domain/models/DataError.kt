package com.tangem.feature.swap.domain.models

sealed class DataError {
    object UnknownError : DataError()
    data class Error(val message: String) : DataError()
}

fun mapErrors(error: String?): DataError {
    return error?.let { DataError.Error(it) } ?: DataError.UnknownError
}
