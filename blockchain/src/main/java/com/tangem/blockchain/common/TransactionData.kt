package com.tangem.blockchain.common

import java.util.*

data class TransactionData(
        val amount: Amount,
        val fee: Amount?,
        val sourceAddress: String,
        val destinationAddress: String,
        val contractAddress: String? = null,
        var status: TransactionStatus = TransactionStatus.Unconfirmed,
        var date: Calendar? = null,
        val hash: String? = null
)


enum class TransactionStatus { Confirmed, Unconfirmed }

enum class TransactionError { WrongAmount, WrongFee, WrongTotal }