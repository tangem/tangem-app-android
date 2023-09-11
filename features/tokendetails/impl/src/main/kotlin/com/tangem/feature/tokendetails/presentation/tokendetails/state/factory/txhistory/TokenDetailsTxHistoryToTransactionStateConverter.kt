package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.converter.Converter
import com.tangem.utils.toBriefAddressFormat
import com.tangem.utils.toFormattedCurrencyString
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatterBuilder
import java.math.BigDecimal
import java.util.Locale

internal class TokenDetailsTxHistoryToTransactionStateConverter(
    private val symbol: String,
    private val decimals: Int,
) : Converter<TxHistoryItem, TransactionState> {

    /** Example, 13:35 */
    private val timeFormatter by lazy {
        DateTimeFormatterBuilder()
            .appendHourOfDay(1)
            .appendLiteral(':')
            .appendMinuteOfHour(2)
            .toFormatter()
            .withLocale(Locale.getDefault())
    }

    override fun convert(value: TxHistoryItem): TransactionState {
        return when (value.type) {
            TxHistoryItem.TransactionType.Transfer -> {
                when (val direction = value.direction) {
                    is TxHistoryItem.TransactionDirection.Incoming -> {
                        createIncomingTransferTransaction(value, direction)
                    }
                    is TxHistoryItem.TransactionDirection.Outgoing -> {
                        createOutgoingTransferTransaction(value, direction)
                    }
                }
            }
        }
    }

    private fun createIncomingTransferTransaction(
        item: TxHistoryItem,
        direction: TxHistoryItem.TransactionDirection.Incoming,
    ): TransactionState {
        return when (item.status) {
            TxHistoryItem.TxStatus.Confirmed -> TransactionState.Receive(
                txHash = item.txHash,
                address = direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = timeFormatter.print(DateTime(item.timestampInMillis, DateTimeZone.getDefault())),
            )
            TxHistoryItem.TxStatus.Unconfirmed -> TransactionState.Receiving(
                txHash = item.txHash,
                address = direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = timeFormatter.print(DateTime(item.timestampInMillis, DateTimeZone.getDefault())),
            )
        }
    }

    private fun createOutgoingTransferTransaction(
        item: TxHistoryItem,
        direction: TxHistoryItem.TransactionDirection.Outgoing,
    ): TransactionState {
        return when (item.status) {
            TxHistoryItem.TxStatus.Confirmed -> TransactionState.Send(
                txHash = item.txHash,
                address = direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = timeFormatter.print(DateTime(item.timestampInMillis, DateTimeZone.getDefault())),
            )
            TxHistoryItem.TxStatus.Unconfirmed -> TransactionState.Sending(
                txHash = item.txHash,
                address = direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = timeFormatter.print(DateTime(item.timestampInMillis, DateTimeZone.getDefault())),
            )
        }
    }

    private fun BigDecimal.toCryptoCurrencyFormat(): String {
        return toFormattedCurrencyString(currency = symbol, decimals = decimals)
    }

    private fun TxHistoryItem.TransactionDirection.extractAddress(): TextReference = when (val addr = address) {
        TxHistoryItem.Address.Multiple -> TextReference.Res(R.string.transaction_history_multiple_addresses)
        is TxHistoryItem.Address.Single -> TextReference.Str(addr.rawAddress.toBriefAddressFormat())
    }
}