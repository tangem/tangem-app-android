package com.tangem.tap.domain.model

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet

data class PendingTransaction(
    val transactionData: TransactionData.Uncompiled,
    val type: PendingTransactionType,
) {
    val address: String? = when (type) {
        PendingTransactionType.Incoming -> nullIfUnknown(transactionData.sourceAddress)
        PendingTransactionType.Outgoing -> nullIfUnknown(transactionData.destinationAddress)
        PendingTransactionType.Unknown -> null
    }

    val currency: String = transactionData.amount.currencySymbol

    private fun nullIfUnknown(address: String): String? = if (address == "unknown") null else address
}

enum class PendingTransactionType { Incoming, Outgoing, Unknown }

fun TransactionData.Uncompiled.toPendingTransaction(walletAddress: String): PendingTransaction? {
    if (this.status == TransactionStatus.Confirmed) return null

    val type: PendingTransactionType = when {
        this.sourceAddress == walletAddress -> PendingTransactionType.Outgoing
        this.destinationAddress == walletAddress -> PendingTransactionType.Incoming
        else -> PendingTransactionType.Unknown
    }
    return PendingTransaction(this, type)
}

fun List<TransactionData.Uncompiled>.toPendingTransactions(walletAddress: String): List<PendingTransaction> {
    return this.mapNotNull { it.toPendingTransaction(walletAddress) }
}

fun Wallet.getPendingTransactions(type: PendingTransactionType? = null): List<PendingTransaction> {
    val txs = recentTransactions.toPendingTransactions(address)
    return when (type) {
        null -> txs
        else -> txs.filter { it.type == type }
    }
}

fun Wallet.hasPendingTransactions(): Boolean {
    return getPendingTransactions().isNotEmpty()
}
