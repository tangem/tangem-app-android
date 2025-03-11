package com.tangem.domain.txhistory.models

sealed class TxHistoryListError : Throwable() {
    data class DataError(override val cause: Throwable) : TxHistoryListError()
}
