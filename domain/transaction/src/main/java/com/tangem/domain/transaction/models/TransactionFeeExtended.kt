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
     * When it is a [GaslessFeePlan.TokenPay] with an approval or a yield withdraw, the send step builds a batch.
     */
    val gaslessFeePlan: GaslessFeePlan? = null,
    /**
     * Per-call gas limits of the user's calls, in call order, each bound into the v2 EIP-712 hash
     * ([GaslessTransactionData.Transaction.gasLimit]). Non-null only on the EVM token-fee (gasless) path;
     * its size must match the number of transactions sent in the gasless payload.
     */
    val callGasLimits: List<BigInteger>? = null,
    /**
     * Per-call gas limit for the appended yield-withdraw sub-call in a batch. Non-null whenever the fee token is
     * in yield; used as the withdraw transaction's [GaslessTransactionData.Transaction.gasLimit] when
     * [GaslessFeePlan.TokenPay.yieldWithdraw] is set.
     */
    val withdrawGasLimit: BigInteger? = null,
    /**
     * Per-call gas limit for the approve sub-call that precedes the main transaction in a batch. Non-null only
     * when [GaslessFeePlan.TokenPay.approval] is set.
     */
    val approveGasLimit: BigInteger? = null,
    /**
     * Non-null exactly when the fee is gasless — every resolver quoting one must set it. Null means the
     * fee is paid in the network's own coin.
     */
    val gaslessProtocolData: GaslessProtocolData? = null,
    /**
     * Native-coin estimation of the same transaction. Equals [transactionFee] when the fee is paid in
     * the native coin. Consumers use it to decide whether the native coin has enough balance to be
     * offered as a fee payment option — see
     * [com.tangem.domain.transaction.usecase.gasless.GetAvailableFeeTokensUseCase].
     */
    val nativeFee: TransactionFee? = null,
)

val TransactionFeeExtended.isGasless: Boolean
    get() = gaslessProtocolData != null