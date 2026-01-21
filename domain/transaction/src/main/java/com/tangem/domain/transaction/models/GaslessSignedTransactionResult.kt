package com.tangem.domain.transaction.models

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
 * @property txHash sent tx hash
 */
data class GaslessSignedTransactionResult(
    val txHash: String,
) {
    init {
        require(txHash.isNotBlank()) { "Tx hash must not be blank" }
    }
}