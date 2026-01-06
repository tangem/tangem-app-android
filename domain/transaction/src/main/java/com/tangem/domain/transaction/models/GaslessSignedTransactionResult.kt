package com.tangem.domain.transaction.models

import java.math.BigInteger

/**
 * Result of gasless transaction signing from the gasless service.
 * Contains the fully signed transaction ready for broadcasting and gas parameters.
 *
 * After the user signs the gasless transaction data locally, the service:
 * 1. Validates the signature
 * 2. Adds its own signature for fee delegation
 * 3. Constructs the final transaction
 * 4. Returns this signed transaction with gas parameters
 *
 * @property signedTransaction complete signed transaction in RLP-encoded hex format (0x...)
 *                             ready to be broadcast to the blockchain network
 * @property gasLimit maximum amount of gas units allocated for the transaction
 * @property maxFeePerGas maximum total fee per gas unit (base fee + priority fee) in wei
 * @property maxPriorityFeePerGas maximum priority fee (tip) per gas unit in wei for EIP-1559
 */
data class GaslessSignedTransactionResult(
    val signedTransaction: String,
    val gasLimit: BigInteger,
    val maxFeePerGas: BigInteger,
    val maxPriorityFeePerGas: BigInteger,
) {
    init {
        require(signedTransaction.isNotBlank()) { "Signed transaction must not be blank" }
        require(signedTransaction.startsWith("0x")) { "Signed transaction must be in hex format with 0x prefix" }
        require(gasLimit > BigInteger.ZERO) { "Gas limit must be positive" }
        require(maxFeePerGas > BigInteger.ZERO) { "Max fee per gas must be positive" }
        require(maxPriorityFeePerGas >= BigInteger.ZERO) { "Max priority fee per gas must be non-negative" }
        require(maxPriorityFeePerGas <= maxFeePerGas) {
            "Max priority fee per gas cannot exceed max fee per gas"
        }
    }
}