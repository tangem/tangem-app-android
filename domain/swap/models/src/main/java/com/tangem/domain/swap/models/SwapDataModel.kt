package com.tangem.domain.swap.models

import java.math.BigDecimal
import java.math.BigInteger

data class SwapDataModel(
    val toTokenAmount: BigDecimal,
    val transaction: SwapDataTransactionModel,
)

sealed class SwapDataTransactionModel {

    abstract val fromAmount: BigDecimal
    abstract val toAmount: BigDecimal
    abstract val txValue: String?
    abstract val txId: String
    abstract val txTo: String
    abstract val txExtraId: String?

    /**
     * @param txValue amount for tx, should use native coin decimals, this value will send as native amount in tx
     */
    data class DEX(
        override val fromAmount: BigDecimal,
        override val toAmount: BigDecimal,
        override val txValue: String?,
        override val txId: String,
        override val txTo: String,
        override val txExtraId: String?,
        val txFrom: String,
        val txData: String,
        val otherNativeFeeWei: BigDecimal?,
        val gas: BigInteger,
    ) : SwapDataTransactionModel()

    data class CEX(
        override val fromAmount: BigDecimal,
        override val toAmount: BigDecimal,
        override val txValue: String?,
        override val txId: String,
        override val txTo: String,
        override val txExtraId: String?,
        val externalTxId: String,
        val externalTxUrl: String,
        val txExtraIdName: String?,
    ) : SwapDataTransactionModel()
}