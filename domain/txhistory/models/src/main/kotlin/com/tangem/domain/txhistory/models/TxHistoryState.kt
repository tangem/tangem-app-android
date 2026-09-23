package com.tangem.domain.txhistory.models

sealed class TxHistoryState {

    sealed class Success : TxHistoryState() {
        object Empty : Success()
        data class HasTransactions(val txCount: Int) : Success()
    }

    sealed class Failed : TxHistoryState() {
        data class FetchError(val exception: Exception) : Failed()
    }

    object NotImplemented : TxHistoryState()

    /**
     * The address the history is read at is not known yet.
     *
     * A joint account reads its history by the Safe address, which is resolved from data arriving after the account
     * itself, so an early request legitimately finds nothing to read at. This is a step before loading rather than a
     * failure: the same request succeeds once the address is there.
     */
    object AddressNotReady : TxHistoryState()
}