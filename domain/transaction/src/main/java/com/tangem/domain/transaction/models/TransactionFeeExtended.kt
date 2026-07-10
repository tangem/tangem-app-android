package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigInteger

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
    /**
     * Per-call gas limit for the user's main transaction, bound into the v2 EIP-712 hash
     * ([GaslessTransactionData.Transaction.gasLimit]). Non-null only on the token-fee (gasless) path,
     * where it equals the estimated execution gas of the user's transaction.
     */
    val mainTransactionGasLimit: BigInteger? = null,
    /**
     * Per-call gas limit for the appended yield-withdraw sub-call in a batch. Non-null only when the
     * fee is paid via [GaslessFeePlan.TokenPayWithYieldWithdraw]; used as the withdraw transaction's
     * [GaslessTransactionData.Transaction.gasLimit].
     */
    val withdrawGasLimit: BigInteger? = null,
)