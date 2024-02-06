package com.tangem.domain.tokens.model.blockchains

import java.math.BigDecimal

/**
 * Model stores utxo limits
 *
 * @property maxLimit utxo limit
 * @property maxAmount max amount
 */
data class UtxoAmountLimit(
    val maxLimit: BigDecimal,
    val maxAmount: BigDecimal,
)