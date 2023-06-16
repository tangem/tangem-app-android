package com.tangem.domain.tokens.model

import java.math.BigDecimal

data class NetworkStatus(
    val networkId: Network.ID,
    val value: Status,
) {

    sealed class Status {
        abstract val amounts: Map<Token.ID, BigDecimal>?
    }

    object Unreachable : Status() {
        override val amounts: Map<Token.ID, BigDecimal>? = null
    }

    object MissedDerivation : Status() {
        override val amounts: Map<Token.ID, BigDecimal>? = null
    }

    data class TransactionInProgress(override val amounts: Map<Token.ID, BigDecimal>) : Status()

    data class Verified(override val amounts: Map<Token.ID, BigDecimal>) : Status()

    data class NoAccount(val amountToCreateAccount: BigDecimal) : Status() {
        override val amounts: Map<Token.ID, BigDecimal>? = null
    }
}
