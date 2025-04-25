package com.tangem.domain.transaction.error

sealed class ValidateMemoError {
    data object InvalidMemo : ValidateMemoError()
    data class DataError(val throwable: Throwable) : ValidateMemoError()
}