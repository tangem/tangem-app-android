package com.tangem.lib.crypto.models.txhistory

sealed class ProxyTransactionHistoryState {

    sealed class Success : ProxyTransactionHistoryState() {
        object Empty : Success()
        data class HasTransactions(val txCount: Int) : Success()
    }

    sealed class Failed : ProxyTransactionHistoryState() {
        data class FetchError(val exception: Exception) : Failed()
    }

    object NotImplemented : ProxyTransactionHistoryState()
}
