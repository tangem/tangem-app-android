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
        direction = value.direction.toDomain(),
        status = when (value.status) {
            TransactionStatus.Confirmed -> TxHistoryItem.TxStatus.Confirmed
            TransactionStatus.Unconfirmed -> TxHistoryItem.TxStatus.Unconfirmed
        },
        type = when (value.type) {
            TransactionHistoryItem.TransactionType.Transfer -> TxHistoryItem.TransactionType.Transfer
        },
        amount = requireNotNull(value.amount.value) { "Transaction amount value must not be null" },
    )

    private fun TransactionHistoryItem.TransactionDirection.toDomain() = when (this) {
        is TransactionHistoryItem.TransactionDirection.Incoming ->
            TxHistoryItem.TransactionDirection.Incoming(address.toDomain())
        is TransactionHistoryItem.TransactionDirection.Outgoing ->
            TxHistoryItem.TransactionDirection.Outgoing(address.toDomain())
    }

    private fun TransactionHistoryItem.Address.toDomain(): TxHistoryItem.Address = when (this) {
        TransactionHistoryItem.Address.Multiple -> TxHistoryItem.Address.Multiple
        is TransactionHistoryItem.Address.Single -> TxHistoryItem.Address.Single(rawAddress = rawAddress)
    }
}