package com.tangem.domain.jointaccount.model

import java.math.BigInteger

/** An `execTransaction` read back from the calldata of an executed transaction */
data class SafeExecution(
    val call: SafeInnerCall,
    val operation: Int,
    val refund: SafeRefund,
    val signaturesCount: Int,
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