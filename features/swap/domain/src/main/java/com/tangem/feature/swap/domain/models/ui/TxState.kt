package com.tangem.feature.swap.domain.models.ui

sealed class TxState {

    data class TxSent(
        val fromAmount: String? = null,
        val toAmount: String? = null,
        val txAddress: String,
    ) : TxState()

    object UserCancelled : TxState()
    object BlockchainError : TxState()
    object TangemSdkError : TxState()
    object UnknownError : TxState()
}
