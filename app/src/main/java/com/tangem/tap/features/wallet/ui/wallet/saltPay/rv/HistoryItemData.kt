package com.tangem.tap.features.wallet.ui.wallet.saltPay.rv

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.common.extensions.guard
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.features.wallet.models.PendingTransactionType
import java.text.SimpleDateFormat
import java.util.*

/**
* [REDACTED_AUTHOR]
 */
sealed class HistoryItemData(
    val viewType: Int,
    val itemId: Long,
) {
    data class Date(val date: String) : HistoryItemData(0, date.hashCode().toLong())
    data class TransactionData(val data: HistoryTransactionData) : HistoryItemData(1, data.hash.hashCode().toLong())
}

data class HistoryTransactionData(
    private val transactionData: TransactionData,
    private val walletAddress: String,
) {

    val isInProgress: Boolean = transactionData.status == TransactionStatus.Unconfirmed

    val transactionType: PendingTransactionType = when {
        transactionData.sourceAddress.lowercase() == walletAddress.lowercase() -> PendingTransactionType.Outgoing
        transactionData.destinationAddress.lowercase() == walletAddress.lowercase() -> PendingTransactionType.Incoming
        else -> PendingTransactionType.Unknown
    }

    val hash: String = transactionData.hash?.let {
        "${it.substring(0..5)}...${it.substring(it.length - 4)}"
    } ?: ""

    val time: String = transactionData.date?.let {
        SimpleDateFormat("hh:MM").format(it.time)
    } ?: "00:00"

    val date: String = calculateDate()

    val txSign: String = when (transactionType) {
        PendingTransactionType.Incoming -> "+"
        PendingTransactionType.Outgoing -> "-"
        PendingTransactionType.Unknown -> ""
    }

    val amountValue: String = transactionData.amount.value
        ?.toFormattedCurrencyString(8, transactionData.amount.currencySymbol) ?: "0"

    private fun calculateDate(): String {
        val calendarTx = transactionData.date.guard { return "Unknown" }

        val calendarNow = Calendar.getInstance()
        val oldDateFormatter = SimpleDateFormat("dd.MM.yyyy")

        val date = if (calendarTx.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR)) {
            val dayOfYearTx = calendarTx.get(Calendar.DAY_OF_YEAR)
            val dayOfYearNow = calendarNow.get(Calendar.DAY_OF_YEAR)
            when {
                dayOfYearNow == dayOfYearTx -> "Today"
                dayOfYearNow - dayOfYearTx == 1 -> "Tomorrow"
                else -> oldDateFormatter.format(calendarTx.time)
            }
        } else {
            oldDateFormatter.format(calendarTx.time)
        }

        return date
    }
}
