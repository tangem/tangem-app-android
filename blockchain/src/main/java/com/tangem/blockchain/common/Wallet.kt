package com.tangem.blockchain.common

import java.math.BigDecimal
import java.util.*

interface Wallet {
    val config: WalletConfig
    val address: String
    val exploreUrl: String?
    val shareUrl: String?
}

class WalletConfig(
        val allowFeeSelection: Boolean,
        val allowFeeInclusion: Boolean,
        var allowExtract: Boolean = false,
        var allowLoad: Boolean = false
)

data class Amount(
        val currencySymbol: String,
        val value: BigDecimal?,
        val address: String,
        val decimals: Int,
        val type: AmountType = AmountType.Coin
        )

data class TransactionData(
        val amount: Amount,
        val fee: Amount?,
        val sourceAddress: String,
        val destinationAddress: String
)

enum class AmountType { Coin, Token, Reserve }

enum class ValidationError { WrongAmount, WrongFee, WrongTotal }

interface TransactionValidator {
    fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<ValidationError>
}