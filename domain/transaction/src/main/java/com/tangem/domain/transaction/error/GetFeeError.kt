package com.tangem.domain.transaction.error

sealed class GetFeeError {
    data class DataError(val cause: Throwable?) : GetFeeError()
    object UnknownError : GetFeeError()
}