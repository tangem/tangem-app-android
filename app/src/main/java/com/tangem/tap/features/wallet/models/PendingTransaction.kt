package com.tangem.tap.features.wallet.models

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.tap.common.extensions.toFormattedString
import java.math.BigDecimal

data class PendingTransaction(
    val transactionData: TransactionData,
    val type: PendingTransactionType,
) {
    val address: String? = when (type) {
        PendingTransactionType.Incoming -> nullIfUnknown(transactionData.sourceAddress)
        PendingTransactionType.Outgoing -> nullIfUnknown(transactionData.destinationAddress)
        PendingTransactionType.Unknown -> null
    }

    val amountValue: BigDecimal? = transactionData.amount.value

    val amountValueUi: String? = amountValue?.toFormattedString(transactionData.amount.decimals)

    val currency: String = transactionData.amount.currencySymbol

    fun nullIfUnknown(address: String): String? = if (address == "unknown") null else address
}

enum class PendingTransactionType { Incoming, Outgoing, Unknown }

fun TransactionData.toPendingTransaction(walletAddress: String): PendingTransaction? {
    if (this.status == TransactionStatus.Confirmed) return null

    val type: PendingTransactionType = when {
        this.sourceAddress == walletAddress -> PendingTransactionType.Outgoing
        this.destinationAddress == walletAddress -> PendingTransactionType.Incoming
        else -> PendingTransactionType.Unknown
    }
    return PendingTransaction(this, type)
}

fun List<TransactionData>.toPendingTransactions(walletAddress: String): List<PendingTransaction> {
    return this.mapNotNull { it.toPendingTransaction(walletAddress) }
}

fun List<PendingTransaction>.removeUnknownTransactions(): List<PendingTransaction> {
    return this.filter { it.type != PendingTransactionType.Unknown }
}

fun List<PendingTransaction>.filterByCoin(): List<PendingTransaction> {
    return this.filter { it.transactionData.amount.type == AmountType.Coin }
}

fun List<PendingTransaction>.filterByToken(token: Token): List<PendingTransaction> {
    return this.filter { it.transactionData.amount.currencySymbol == token.symbol }
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
