package com.tangem.feature.swap.domain.models

sealed class DataError {
    object NoError : DataError()
    data class UnknownError(val message: String) : DataError()
    object InsufficientLiquidity : DataError()
}

fun mapErrors(error: String?): DataError {
    return if (error == null) {
        DataError.NoError
    } else when (error) {
        INSUFFICIENT_LIQUIDITY_ERROR -> DataError.InsufficientLiquidity
        else -> DataError.UnknownError(error)
    }
}

private const val INSUFFICIENT_LIQUIDITY_ERROR = "insufficient liquidity"
