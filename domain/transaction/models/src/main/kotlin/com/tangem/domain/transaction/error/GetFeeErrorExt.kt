package com.tangem.domain.transaction.error

/**
 * Refusals of the account activation the gasless service sponsors for a non-activated Tron sender.
 *
 * They must not fall back to the native fee: an account that still has to be activated cannot pay a
 * TRX fee either, so the fallback would replace an actionable message with an unpayable fee.
 */
fun GetFeeError.isTronActivationRefusal(): Boolean = when (this) {
    is GetFeeError.GaslessError.TronActivationNotEnoughFunds,
    is GetFeeError.GaslessError.TronActivationRateLimited,
    -> true
    else -> false
}