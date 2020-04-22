package com.tangem.blockchain.common

import java.math.BigDecimal
import java.util.*

class Wallet(
        val blockchain: Blockchain,
        val address: String,
        val token: Token? = null
) {
    val exploreUrl: String
    val shareUrl: String
    val transactions: MutableList<TransactionData> = mutableListOf()
    val amounts: MutableMap<AmountType, Amount> = mutableMapOf()

    init {
        setAmount(Amount(null, blockchain, address))
        if (token != null) setAmount(Amount(token))

        exploreUrl = blockchain.getExploreUrl(address, token)
        shareUrl = blockchain.getShareUri(address)
    }

    fun setAmount(amount: Amount) {
        amounts[amount.type] = amount
    }

    fun setCoinValue(value: BigDecimal) {
        val amount = Amount(value, blockchain, address)
        setAmount(amount)
    }

    fun setTokenValue(value: BigDecimal) {
        if (token != null) {
            val amount = Amount(token, value)
            setAmount(amount)
        }
    }

    fun setReserveValue(value: BigDecimal) {
        val amount = Amount(value, blockchain, address, AmountType.Reserve)
        setAmount(amount)
    }

    fun addTransaction(transaction: TransactionData) {
        transactions.add(transaction.copy(date = Calendar.getInstance()))
    }

    fun addIncomingTransaction() {
        val dummyAmount = Amount(null, blockchain)
        val transaction = TransactionData(dummyAmount, dummyAmount,
                "unknown", address, date = Calendar.getInstance()
        )
        transactions.add(transaction)
    }

    fun fundsAvailable(amountType: AmountType): BigDecimal {
        return amounts[amountType]?.value ?: BigDecimal.ZERO
    }

//    fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<ValidationError> {
//        TODO("not implemented")
//    }
}

enum class AmountType { Coin, Token, Reserve }

interface TransactionValidator {
    fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError>
}