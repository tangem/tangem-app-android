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
}