package com.tangem.feature.swap.domain.models.data

enum class DataError {
    NO_ERROR,
    UNKNOWN_ERROR,
    INSUFFICIENT_LIQUIDITY
}

fun mapErrors(error: String?): DataError {
    return if (error == null) {
        DataError.UNKNOWN_ERROR
    } else when (error) {
        INSUFFICIENT_LIQUIDITY_ERROR -> DataError.INSUFFICIENT_LIQUIDITY
        else -> DataError.UNKNOWN_ERROR
    }
}

private const val INSUFFICIENT_LIQUIDITY_ERROR = "insufficient liquidity"