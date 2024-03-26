package com.tangem.feature.swap.domain.models.ui

import com.tangem.feature.swap.domain.models.DataError
import java.math.BigDecimal

//rename this?
sealed class TxState {

    data class TxSent(
        val fromAmount: String? = null,
        val fromAmountValue: BigDecimal? = null,
        val toAmount: String? = null,
        val toAmountValue: BigDecimal? = null,
        val txHash: String,
        val txExternalUrl: String? = null,
        val timestamp: Long,
    ) : TxState()

    data object UserCancelled : TxState()

    data object BlockchainError : TxState()

    data object TangemSdkError : TxState()

    data object NetworkError : TxState()

    data object UnknownError : TxState()

    data class ExpressError(val dataError: DataError) : TxState()
}
