package com.tangem.feature.swap.domain.models.ui

import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.feature.swap.domain.models.ExpressDataError
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

    data object DemoMode : SwapTransactionState()

    sealed class Error : SwapTransactionState() {
        data class TransactionError(val error: SendTransactionError?) : Error()

        data class ExpressError(val error: ExpressDataError) : Error()

        data object UnknownError : Error()
    }
}