package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.Address
import com.tangem.utils.converter.Converter
import timber.log.Timber
import java.math.BigDecimal

/**
 * Convert [TransactionData] to [TxHistoryItem]
 *
 * @property walletAddresses wallet addresses
 *
[REDACTED_AUTHOR]
 */
internal class TransactionDataToTxHistoryItemConverter(
    private val walletAddresses: Set<Address>,
) : Converter<TransactionData, TxHistoryItem?> {

    override fun convert(value: TransactionData): TxHistoryItem? {
        val hash = value.hash ?: return null
        val millis = value.date?.timeInMillis ?: return null
        val amount = getTransactionAmountValue(value.amount) ?: return null
        val isOutgoing = value.sourceAddress in walletAddresses.map(Address::value)

        return TxHistoryItem(
            txHash = hash,
            timestampInMillis = millis,
            isOutgoing = isOutgoing,
            destinationType = TxHistoryItem.DestinationType.Single(
                addressType = TxHistoryItem.AddressType.User(value.destinationAddress),
            ),
            sourceType = TxHistoryItem.SourceType.Single(value.sourceAddress),
            interactionAddressType = TxHistoryItem.InteractionAddressType.User(
                address = if (isOutgoing) value.destinationAddress else value.sourceAddress,
            ),
            status = when (value.status) {
                TransactionStatus.Confirmed -> TxHistoryItem.TransactionStatus.Confirmed
                TransactionStatus.Unconfirmed -> TxHistoryItem.TransactionStatus.Unconfirmed
            },
            type = TxHistoryItem.TransactionType.Transfer,
            amount = amount,
        )
    }

    private fun getTransactionAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            Timber.w("Transaction amount must not be null: ${amount.currencySymbol}")
        }

        return value
    }
}