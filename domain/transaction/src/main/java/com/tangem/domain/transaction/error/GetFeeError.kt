package com.tangem.domain.transaction.error

sealed class GetFeeError {
    object DataError : GetFeeError()
}