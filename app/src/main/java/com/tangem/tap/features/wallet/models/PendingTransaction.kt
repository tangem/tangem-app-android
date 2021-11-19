package com.tangem.tap.features.wallet.models

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.domain.extensions.toSendableAmounts

data class PendingTransaction(
    val address: String?,
    val amount: String?,
    val currency: String,
    val type: PendingTransactionType
)

enum class PendingTransactionType { Incoming, Outgoing, Unknown }

fun TransactionData.toPendingTransaction(walletAddress: String): PendingTransaction? {
    if (this.status == TransactionStatus.Confirmed) return null

    val type: PendingTransactionType = when {
        this.sourceAddress == walletAddress -> {
            PendingTransactionType.Outgoing
        }
        this.destinationAddress == walletAddress -> {
            PendingTransactionType.Incoming
        }
        else -> {
            PendingTransactionType.Unknown
        }
    }
    val address = if (this.sourceAddress == walletAddress) {
        this.destinationAddress
    } else {
        this.sourceAddress
    }

    return PendingTransaction(
        if (address == "unknown") null else address,
        this.amount.value?.toFormattedString(amount.decimals),
        this.amount.currencySymbol,
        type
    )
}

fun List<TransactionData>.toPendingTransactions(walletAddress: String): List<PendingTransaction> {
    return this.mapNotNull { it.toPendingTransaction(walletAddress) }
}

fun List<PendingTransaction>.removeUnknownTransactions(): List<PendingTransaction> {
    return this.filter { it.type != PendingTransactionType.Unknown }
}

fun TransactionData.toPendingTransactionForToken(token: Token, walletAddress: String): PendingTransaction? {
    if (this.amount.currencySymbol != token.symbol) return null
    return this.toPendingTransaction(walletAddress)
}

fun List<TransactionData>.toPendingTransactionsForToken(token: Token, walletAddress: String): List<PendingTransaction> {
    return this.mapNotNull { it.toPendingTransactionForToken(token, walletAddress) }
}

fun Wallet.getPendingTransactions(): List<PendingTransaction> {
    return recentTransactions.toPendingTransactions(address)
}

fun Wallet.hasPendingTransactions(): Boolean {
    return getPendingTransactions().isNotEmpty()
}

fun Wallet.hasSendableAmountsOrPendingTransactions(): Boolean {
    return hasPendingTransactions() || amounts.toSendableAmounts().isNotEmpty()
}