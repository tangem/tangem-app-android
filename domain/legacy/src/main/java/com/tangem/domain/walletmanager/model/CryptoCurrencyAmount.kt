package com.tangem.domain.walletmanager.model

import java.math.BigDecimal

sealed class CryptoCurrencyAmount {

    abstract val value: BigDecimal

    data class Coin(override val value: BigDecimal) : CryptoCurrencyAmount()

    data class Token(
        val tokenContractAddress: String,
        override val value: BigDecimal,
    ) : CryptoCurrencyAmount()
}