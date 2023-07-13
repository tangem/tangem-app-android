package com.tangem.domain.tokens.model

import java.math.BigDecimal

data class TokenStatus(
    val id: Token.ID,
    val networkId: Network.ID,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val iconUrl: String?,
    val isCoin: Boolean,
    val value: Status,
) {

    sealed class Status {
        open val amount: BigDecimal? = null
        open val fiatAmount: BigDecimal? = null
        open val priceChange: BigDecimal? = null
        open val hasTransactionsInProgress: Boolean = false
    }

    object Loading : Status()

    object Unreachable : Status()

    object MissedDerivation : Status()

    object NoAccount : Status()

    data class Loaded(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal,
        override val priceChange: BigDecimal,
        override val hasTransactionsInProgress: Boolean,
    ) : Status()

    data class Custom(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal?,
        override val priceChange: BigDecimal?,
        override val hasTransactionsInProgress: Boolean,
    ) : Status()
}