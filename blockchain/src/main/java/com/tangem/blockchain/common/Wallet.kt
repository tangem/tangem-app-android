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
        var value: BigDecimal? = null,
        val address: String? = null,
        val decimals: Byte,
        val type: AmountType = AmountType.Coin
) {
    constructor(
            value: BigDecimal?,
            blockchain: Blockchain,
            address: String? = null,
            type: AmountType = AmountType.Coin
    ) : this(blockchain.currency, value, address, blockchain.decimals, type)

    constructor(token: Token, value: BigDecimal? = null) :
            this(token.symbol, value, token.contractAddress, token.decimals, AmountType.Token)
}

data class TransactionData(
        val amount: Amount,
        val fee: Amount?,
        val sourceAddress: String,
        val destinationAddress: String,
        var status: TransactionStatus = TransactionStatus.Unconfirmed,
        var date: Calendar? = null
)

enum class AmountType { Coin, Token, Reserve }

enum class TransactionStatus { Confirmed, Unconfirmed }

enum class ValidationError { WrongAmount, WrongFee, WrongTotal }

interface TransactionValidator {
    fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<ValidationError>
}