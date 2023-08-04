package com.tangem.domain.txhistory.error

sealed class TxHistoryListError : Throwable() {
    data class DataError(override val cause: Throwable) : TxHistoryListError()
}
