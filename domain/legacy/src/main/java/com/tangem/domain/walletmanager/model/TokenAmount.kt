package com.tangem.domain.walletmanager.model

import java.math.BigDecimal

sealed class TokenAmount {

    abstract val value: BigDecimal

    data class Coin(override val value: BigDecimal) : TokenAmount()

    data class Token(
        val tokenContractAddress: String,
        override val value: BigDecimal,
    ) : TokenAmount()
}
