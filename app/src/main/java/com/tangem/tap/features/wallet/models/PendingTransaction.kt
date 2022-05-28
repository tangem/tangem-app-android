package com.tangem.tap.features.wallet.models

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.tap.common.extensions.toFormattedString
import java.math.BigDecimal

data class PendingTransaction(
    val address: String?,
    val amount: BigDecimal?,
    val amountUi: String?,
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
        this.amount.value,
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

fun Wallet.getPendingTransactions(type: PendingTransactionType? = null): List<PendingTransaction> {
    val txs = recentTransactions.toPendingTransactions(address)
    return when (type) {
        null -> txs
        else -> txs.filter { it.type == type }
    }
}

fun Wallet.getPendingTransactions(token: Token): List<PendingTransaction> {
    return recentTransactions.mapNotNull { it.toPendingTransactionForToken(token, address) }
}

fun Wallet.hasPendingTransactions(): Boolean {
    return getPendingTransactions().isNotEmpty()
}

fun Wallet.hasPendingTransactions(token: Token): Boolean {
    return getPendingTransactions(token).isNotEmpty()
}

fun Wallet.getSendableAmounts(): List<Amount> {
    return amounts.values
        .filter { it.type != AmountType.Reserve }
        .filter { it.isAboveZero() }
}

fun Wallet.hasSendableAmounts(): Boolean {
    return getSendableAmounts().isNotEmpty()
}

fun Wallet.hasSendableAmountsOrPendingTransactions(): Boolean {
    return hasPendingTransactions() || hasSendableAmounts()
}

fun Wallet.isSendableAmount(type: AmountType): Boolean {
    return amounts[type]?.isAboveZero() == true

}

fun Wallet.isSendableAmount(token: Token): Boolean {
    return isSendableAmount(AmountType.Token(token))
}