package com.tangem.domain.txhistory.error

sealed class TxHistoryStateError : Throwable() {

    object TxHistoryNotImplemented : TxHistoryStateError()

    object EmptyTxHistories : TxHistoryStateError()

    data class DataError(override val cause: Throwable) : TxHistoryStateError()
}
