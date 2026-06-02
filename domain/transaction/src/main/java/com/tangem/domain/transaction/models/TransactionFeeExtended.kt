package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency

data class TransactionFeeExtended(
    val transactionFee: TransactionFee,
    val feeTokenId: CryptoCurrency.ID,
    /**
     * Resolved gasless fee strategy. Non-null only for token-paid gasless fees; null for native fee.
     * A null value is semantically equivalent to [GaslessFeePlan.NativePay] — consumers MUST treat them
     * the same. [GaslessFeePlan.NativePay] is produced only by ResolveGaslessFeePlanUseCase.
     * When it is [GaslessFeePlan.TokenPayWithYieldWithdraw], the send step builds a batch transaction.
     */
    val gaslessFeePlan: GaslessFeePlan? = null,
)