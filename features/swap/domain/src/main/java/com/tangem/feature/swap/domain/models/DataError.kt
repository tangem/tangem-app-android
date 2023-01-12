package com.tangem.feature.swap.domain.models

enum class DataError {
    NO_ERROR,
    UNKNOWN_ERROR,
    INSUFFICIENT_LIQUIDITY,
    CANNOT_SYNC
}

fun mapErrors(error: String?): DataError {
    return if (error == null) {
        DataError.UNKNOWN_ERROR
    } else when {
        error == INSUFFICIENT_LIQUIDITY_ERROR -> DataError.INSUFFICIENT_LIQUIDITY
        error.startsWith(CANNOT_SYNC_ERROR) -> DataError.CANNOT_SYNC
        else -> DataError.UNKNOWN_ERROR
    }
}

private const val INSUFFICIENT_LIQUIDITY_ERROR = "insufficient liquidity"
private const val CANNOT_SYNC_ERROR = "cannot sync"
