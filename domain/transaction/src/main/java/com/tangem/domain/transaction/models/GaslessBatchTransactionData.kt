package com.tangem.domain.transaction.models

import java.math.BigInteger

/**
 * Domain model for a gasless BATCH transaction (EIP-712 primaryType `GaslessBatchTransaction`).
 * Reuses [GaslessTransactionData.Transaction] and [GaslessTransactionData.Fee].
 *
 * @property transactions ordered list — index 0 is the user's main transaction, subsequent entries
 *                        are appended operations (e.g. the yield `withdraw`). Executed in array order.
 * @property fee fee payment configuration.
 * @property nonce nonce from the user's contract.
 */
data class GaslessBatchTransactionData(
    val transactions: List<GaslessTransactionData.Transaction>,
    val fee: GaslessTransactionData.Fee,
    val nonce: BigInteger,
)