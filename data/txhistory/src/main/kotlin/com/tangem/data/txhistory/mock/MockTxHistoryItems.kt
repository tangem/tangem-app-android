package com.tangem.data.txhistory.mock

import com.tangem.domain.txhistory.model.TxHistoryItem
import java.math.BigDecimal

internal object MockTxHistoryItems {

    private val txHistoryItem1 = TxHistoryItem(
        txHash = "noster",
        timestamp = System.currentTimeMillis(),
        direction = TxHistoryItem.TransactionDirection.Incoming("address"),
        status = TxHistoryItem.TxStatus.Confirmed,
        type = TxHistoryItem.TransactionType.Transfer,
        amount = BigDecimal("1000000000.5"),
    )

    private val txHistoryItem2 = TxHistoryItem(
        txHash = "noster",
        timestamp = 1689844346000,
        direction = TxHistoryItem.TransactionDirection.Incoming("address2"),
        status = TxHistoryItem.TxStatus.Unconfirmed,
        type = TxHistoryItem.TransactionType.Transfer,
        amount = BigDecimal("1000000000.5"),
    )

    private val txHistoryItem3 = TxHistoryItem(
        txHash = "noster",
        timestamp = 1689757946000,
        direction = TxHistoryItem.TransactionDirection.Outgoing("address3"),
        status = TxHistoryItem.TxStatus.Confirmed,
        type = TxHistoryItem.TransactionType.Transfer,
        amount = BigDecimal("1000000000.5"),
    )

    private val txHistoryItem4 = TxHistoryItem(
        txHash = "noster",
        timestamp = 1689671546000,
        direction = TxHistoryItem.TransactionDirection.Incoming("address4"),
        status = TxHistoryItem.TxStatus.Confirmed,
        type = TxHistoryItem.TransactionType.Transfer,
        amount = BigDecimal("1000000000.5"),
    )

    private val txHistoryItem5 = TxHistoryItem(
        txHash = "noster",
        timestamp = 1689585146000,
        direction = TxHistoryItem.TransactionDirection.Outgoing("address5"),
        status = TxHistoryItem.TxStatus.Unconfirmed,
        type = TxHistoryItem.TransactionType.Transfer,
        amount = BigDecimal("1000000000.5"),
    )

    private val txHistoryItem6 = TxHistoryItem(
        txHash = "noster",
        timestamp = 1689585146000,
        direction = TxHistoryItem.TransactionDirection.Incoming("address6"),
        status = TxHistoryItem.TxStatus.Confirmed,
        type = TxHistoryItem.TransactionType.Transfer,
        amount = BigDecimal("1000000000.5"),
    )

    val txHistoryItems = listOf(
        txHistoryItem1,
        txHistoryItem2,
        txHistoryItem3,
        txHistoryItem4,
        txHistoryItem5,
        txHistoryItem6,
    )
}