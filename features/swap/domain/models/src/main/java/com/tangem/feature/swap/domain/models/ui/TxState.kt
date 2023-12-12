package com.tangem.feature.swap.domain.models.ui

import java.math.BigDecimal

sealed class TxState {

    data class TxSent(
        val fromAmount: String? = null,
        val fromAmountValue: BigDecimal? = null,
        val toAmount: String? = null,
        val toAmountValue: BigDecimal? = null,
        val txAddress: String,
        val txExternalUrl: String? = null,
        val timestamp: Long,
    ) : TxState()

    object UserCancelled : TxState()
    object BlockchainError : TxState()
    object TangemSdkError : TxState()
    object NetworkError : TxState()
    object UnknownError : TxState()
}