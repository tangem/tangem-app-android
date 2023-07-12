package com.tangem.domain.tokens.model

import java.math.BigDecimal

data class NetworkStatus(
    val networkId: Network.ID,
    val value: Status,
) {

    sealed class Status {
        open val amounts: Map<Token.ID, BigDecimal>? = null
    }

    object Unreachable : Status()

    object MissedDerivation : Status()

    data class TransactionInProgress(override val amounts: Map<Token.ID, BigDecimal>) : Status()

    data class Verified(override val amounts: Map<Token.ID, BigDecimal>) : Status()

    data class NoAccount(val amountToCreateAccount: BigDecimal) : Status()
}
