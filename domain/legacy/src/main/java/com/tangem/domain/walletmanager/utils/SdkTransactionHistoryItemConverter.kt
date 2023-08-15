package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem as SdkTransactionHistoryItem

internal class SdkTransactionHistoryItemConverter : Converter<SdkTransactionHistoryItem, TxHistoryItem> {

    override fun convert(value: SdkTransactionHistoryItem): TxHistoryItem = TxHistoryItem(
        txHash = value.txHash,
        timestampInMillis = value.timestamp,
        direction = when (val direction = value.direction) {
            is SdkTransactionHistoryItem.TransactionDirection.Incoming ->
                TxHistoryItem.TransactionDirection.Incoming(direction.from)
            is SdkTransactionHistoryItem.TransactionDirection.Outgoing ->
                TxHistoryItem.TransactionDirection.Outgoing(direction.to)
        },
        status = when (value.status) {
            TransactionStatus.Confirmed -> TxHistoryItem.TxStatus.Confirmed
            TransactionStatus.Unconfirmed -> TxHistoryItem.TxStatus.Unconfirmed
        },
        type = when (value.type) {
            TransactionHistoryItem.TransactionType.Transfer -> TxHistoryItem.TransactionType.Transfer
        },
        amount = requireNotNull(value.amount.value) { "Transaction amount value must not be null" },
    )
}
