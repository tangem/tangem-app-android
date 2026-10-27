package com.tangem.domain.txhistory.models

sealed class TxHistoryStateError : Throwable() {

    class TxHistoryNotImplemented : TxHistoryStateError()

    class EmptyTxHistories : TxHistoryStateError()

    /** Carries [TxHistoryState.AddressNotReady]: nothing failed, the address to read at has not arrived yet */
    class AddressNotReady : TxHistoryStateError()

    data class DataError(override val cause: Throwable) : TxHistoryStateError()
}