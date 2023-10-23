package com.tangem.domain.walletmanager.utils

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
            TransactionHistoryItem.TransactionStatus.Confirmed -> TxHistoryItem.TransactionStatus.Confirmed
            TransactionHistoryItem.TransactionStatus.Failed -> TxHistoryItem.TransactionStatus.Failed
            TransactionHistoryItem.TransactionStatus.Unconfirmed -> TxHistoryItem.TransactionStatus.Unconfirmed
        },
        type = when (val type = value.type) {
            TransactionHistoryItem.TransactionType.Transfer -> TxHistoryItem.TransactionType.Transfer
            TransactionHistoryItem.TransactionType.Approve -> TxHistoryItem.TransactionType.Approve
            is TransactionHistoryItem.TransactionType.Custom -> TxHistoryItem.TransactionType.Custom(type.id)
            TransactionHistoryItem.TransactionType.Deposit -> TxHistoryItem.TransactionType.Deposit
            TransactionHistoryItem.TransactionType.Submit -> TxHistoryItem.TransactionType.Submit
            TransactionHistoryItem.TransactionType.Supply -> TxHistoryItem.TransactionType.Supply
            TransactionHistoryItem.TransactionType.Swap -> TxHistoryItem.TransactionType.Swap
            TransactionHistoryItem.TransactionType.Unoswap -> TxHistoryItem.TransactionType.Unoswap
            TransactionHistoryItem.TransactionType.Withdraw -> TxHistoryItem.TransactionType.Withdraw
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