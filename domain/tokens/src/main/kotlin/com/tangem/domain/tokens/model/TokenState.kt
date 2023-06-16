package com.tangem.domain.tokens.model

import java.math.BigDecimal

data class TokenState(
    val token: Token,
    val value: State,
) {

    sealed class State {
        open val amount: BigDecimal? = null
        open val fiatAmount: BigDecimal? = null
        open val priceChange: BigDecimal? = null
    }

    object Loading : State()

    object Unreachable : State()

    object MissedDerivation : State()

    data class Loaded(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal,
        override val priceChange: BigDecimal,
    ) : State()

    data class TransactionInProgress(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal,
        override val priceChange: BigDecimal,
    ) : State()

    data class NoAccount(
        val amountToCreateAccount: BigDecimal,
    ) : State()
}
