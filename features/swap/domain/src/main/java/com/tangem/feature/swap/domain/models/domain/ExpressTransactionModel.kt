package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount
import java.math.BigDecimal
import java.math.BigInteger

sealed class ExpressTransactionModel {

    abstract val fromAmount: SwapAmount
    abstract val toAmount: SwapAmount
    abstract val txValue: String?
    abstract val txId: String
    abstract val txTo: String
    abstract val txExtraId: String?

    /**
     * @param txValue amount for tx, should use native coin decimals, this value will send as native amount in tx
     * @param gas gas-limit from the express provider; only used by the fee fallback path. Nullable
     *     because providers may omit it.
     * @param allowanceContract spender address for ERC-20 allowance, null when no approval is required.
     */
    data class DEX(
        override val fromAmount: SwapAmount,
        override val toAmount: SwapAmount,
        override val txValue: String?,
        override val txId: String,
        override val txTo: String,
        override val txExtraId: String?,
        val txFrom: String,
        val txData: String,
        val otherNativeFeeWei: BigDecimal?,
        val gas: BigInteger?,
        val allowanceContract: String?,
    ) : ExpressTransactionModel()

    data class CEX(
        override val fromAmount: SwapAmount,
        override val toAmount: SwapAmount,
        override val txValue: String?,
        override val txId: String,
        override val txTo: String,
        override val txExtraId: String?,
        val externalTxId: String,
        val externalTxUrl: String,
        val txExtraIdName: String?,
    ) : ExpressTransactionModel()
}