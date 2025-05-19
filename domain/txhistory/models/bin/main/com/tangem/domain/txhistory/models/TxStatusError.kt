package com.tangem.domain.txhistory.models

sealed class TxStatusError {

    data object EmptyUrlError : TxStatusError()
    data class DataError(val cause: Throwable) : TxStatusError()
}