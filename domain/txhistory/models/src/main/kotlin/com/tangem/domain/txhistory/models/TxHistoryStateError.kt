package com.tangem.domain.txhistory.models

sealed class TxHistoryStateError : Throwable() {

    object TxHistoryNotImplemented : TxHistoryStateError()

    object EmptyTxHistories : TxHistoryStateError()

    data class DataError(override val cause: Throwable) : TxHistoryStateError()
}
