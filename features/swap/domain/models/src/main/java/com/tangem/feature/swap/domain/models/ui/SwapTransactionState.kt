package com.tangem.feature.swap.domain.models.ui

import com.tangem.feature.swap.domain.models.DataError
import java.math.BigDecimal

sealed class SwapTransactionState {

    data class TxSent(
        val fromAmount: String? = null,
        val fromAmountValue: BigDecimal? = null,
        val toAmount: String? = null,
        val toAmountValue: BigDecimal? = null,
        val txHash: String,
        val txExternalUrl: String? = null,
        val timestamp: Long,
    ) : SwapTransactionState()

    data object UserCancelled : SwapTransactionState()

    data object BlockchainError : SwapTransactionState()

    data object TangemSdkError : SwapTransactionState()

    data object NetworkError : SwapTransactionState()

    data object UnknownError : SwapTransactionState()

    data class ExpressError(val dataError: DataError) : SwapTransactionState()
}