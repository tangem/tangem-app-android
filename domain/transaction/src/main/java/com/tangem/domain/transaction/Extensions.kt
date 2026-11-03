package com.tangem.domain.transaction

import arrow.core.raise.Raise
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.transaction.error.GetFeeError

fun Raise<GetFeeError>.raiseIllegalStateError(error: String): Nothing {
    raise(GetFeeError.DataError(IllegalStateException(error)))
}

/**
 * Per-bucket sum of an approve and a swap paid in the native coin as two transactions (same gas price, summed
 * gas limit). Non-EVM fees fall back to the swap fee alone since the integrated approve is EVM-only.
 */
fun combineTransactionFees(approvalFee: TransactionFee, swapFee: TransactionFee): TransactionFee {
    return when {
        approvalFee is TransactionFee.Choosable && swapFee is TransactionFee.Choosable ->
            TransactionFee.Choosable(
                minimum = sumEvmFees(approvalFee.minimum, swapFee.minimum),
                normal = sumEvmFees(approvalFee.normal, swapFee.normal),
                priority = sumEvmFees(approvalFee.priority, swapFee.priority),
            )
        else -> TransactionFee.Single(normal = sumEvmFees(approvalFee.normal, swapFee.normal))
    }
}

private fun sumEvmFees(left: Fee, right: Fee): Fee {
    if (left !is Fee.Ethereum || right !is Fee.Ethereum) return right
    val leftValue = left.amount.value ?: return right
    val rightValue = right.amount.value ?: return right
    val combinedValue = leftValue + rightValue
    val combinedGasLimit = left.gasLimit + right.gasLimit
    val combinedAmount = right.amount.copy(value = combinedValue)
    return when (right) {
        is Fee.Ethereum.EIP1559 -> right.copy(amount = combinedAmount, gasLimit = combinedGasLimit)
        is Fee.Ethereum.Legacy -> right.copy(amount = combinedAmount, gasLimit = combinedGasLimit)
        is Fee.Ethereum.TokenCurrency -> right
    }
}