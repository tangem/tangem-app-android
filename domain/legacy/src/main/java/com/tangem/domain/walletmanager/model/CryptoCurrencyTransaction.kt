package com.tangem.domain.walletmanager.model

import org.joda.time.DateTime
import java.math.BigDecimal

sealed class CryptoCurrencyTransaction {

    abstract val amount: BigDecimal
    abstract val fromAddress: String?
    abstract val toAddress: String?
    abstract val sentAt: DateTime

    data class Coin(
        override val amount: BigDecimal,
        override val fromAddress: String?,
        override val toAddress: String?,
        override val sentAt: DateTime,
    ) : CryptoCurrencyTransaction()

    data class Token(
        val tokenId: String?,
        val tokenContractAddress: String,
        override val amount: BigDecimal,
        override val fromAddress: String?,
        override val toAddress: String?,
        override val sentAt: DateTime,
    ) : CryptoCurrencyTransaction()
}