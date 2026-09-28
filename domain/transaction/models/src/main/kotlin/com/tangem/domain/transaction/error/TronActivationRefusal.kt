package com.tangem.domain.transaction.error

import java.math.BigInteger

/** Why the gasless service refused to sponsor the activation of a non-activated Tron sender. */
sealed interface TronActivationRefusal {

    /** The sender holds less than [minimumAmountRaw] base units of the gasless token. */
    data class BalanceTooLow(val minimumAmountRaw: BigInteger) : TronActivationRefusal

    /**
     * The service rate limits the sponsorship: [isPerWallet] when this wallet used up its own attempts,
     * otherwise the service-wide limit is reached.
     */
    data class RateLimited(val isPerWallet: Boolean) : TronActivationRefusal
}