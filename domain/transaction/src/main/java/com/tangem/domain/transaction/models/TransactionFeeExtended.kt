package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency

data class TransactionFeeExtended(
    val transactionFee: TransactionFee,
    val feeTokenId: CryptoCurrency.ID,
    /**
     * Resolved gasless fee strategy. Non-null only for token-paid gasless fees; null for native fee.
     * When it is [GaslessFeePlan.TokenPayWithYieldWithdraw], the send step builds a batch transaction.
     */
    val gaslessFeePlan: GaslessFeePlan? = null,
)