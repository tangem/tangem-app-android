package com.tangem.domain.transaction.error

import java.math.BigInteger

/**
 * The Tron gasless service refused the quote because the allowances of the compensation token, together with
 * the compensation, need more than the sender holds. All amounts are base units of the compensation token.
 *
 * For an approve [allowanceRaw] is the requested amount and [requiredRaw] adds two compensations to it (its own
 * and the one of the swap that follows). For a swap [allowanceRaw] is what the router and spender may already pull
 * and [requiredRaw] adds the swap compensation to it, with [allowancesRaw] splitting it by router and spender address.
 */
data class TronAllowanceTooHigh(
    val balanceRaw: BigInteger,
    val requiredRaw: BigInteger,
    val allowanceRaw: BigInteger,
    val allowancesRaw: Map<String, BigInteger> = emptyMap(),
)