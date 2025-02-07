package com.tangem.domain.tokens.model.blockchains

import java.math.BigDecimal

/**
 * Model stores utxo limits
 *
 * @property limit Maximum allowed number of UTXO in single transaction
 * @property availableToSpend Allowed amount to participate in single transaction
 * @property availableToSend Amount user can send in single transaction
 */
data class UtxoAmountLimit(
    val limit: BigDecimal,
    val availableToSpend: BigDecimal,
    val availableToSend: BigDecimal?,
)