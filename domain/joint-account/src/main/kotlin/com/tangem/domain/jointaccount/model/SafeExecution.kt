package com.tangem.domain.jointaccount.model

import java.math.BigInteger

/**
 * An `execTransaction` read back from the calldata of an executed transaction. [safe] is the Safe a bundle called;
 * `null` for a direct call, where the Safe is the transaction's own target.
 */
data class SafeExecution(
    val call: SafeInnerCall,
    val operation: Int,
    val refund: SafeRefund,
    val signaturesCount: Int,
    val safe: String? = null,
)

/** [maxPayment] is a cap in wei of [gasToken]: the actual refund depends on the gas used and is not in the calldata */
data class SafeRefund(
    val safeTxGas: BigInteger,
    val baseGas: BigInteger,
    val gasPrice: BigInteger,
    val gasToken: String,
    val refundReceiver: String,
) {
    val maxPayment: BigInteger get() = (safeTxGas + baseGas) * gasPrice
}