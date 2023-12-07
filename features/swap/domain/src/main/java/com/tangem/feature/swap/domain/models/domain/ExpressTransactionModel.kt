package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount

sealed class ExpressTransactionModel {

    abstract val fromAmount: SwapAmount
    abstract val toAmount: SwapAmount
    abstract val txId: String
    abstract val txTo: String

    data class DEX(
        override val fromAmount: SwapAmount,
        override val toAmount: SwapAmount,
        override val txId: String,
        override val txTo: String,
        val txFrom: String,
        val txData: String,
    ) : ExpressTransactionModel()

    data class CEX(
        override val fromAmount: SwapAmount,
        override val toAmount: SwapAmount,
        override val txId: String,
        override val txTo: String,
        val externalTxId: String,
        val externalTxUrl: String,
    ) : ExpressTransactionModel()
}
