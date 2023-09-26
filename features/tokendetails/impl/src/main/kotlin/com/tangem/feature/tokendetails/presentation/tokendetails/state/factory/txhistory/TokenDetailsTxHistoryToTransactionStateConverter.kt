package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.converter.Converter
import com.tangem.utils.toBriefAddressFormat
import com.tangem.utils.toFormattedCurrencyString
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.math.BigDecimal

internal class TokenDetailsTxHistoryToTransactionStateConverter(
    private val symbol: String,
    private val decimals: Int,
) : Converter<TxHistoryItem, TransactionState> {

    override fun convert(value: TxHistoryItem): TransactionState {
        return createTransactionStateItem(item = value)
    }

    // TODO: Finalize transaction types [REDACTED_JIRA]
    private fun createTransactionStateItem(item: TxHistoryItem): TransactionState {
        return when (val type = item.type) {
            TxHistoryItem.TransactionType.Transfer -> mapTransfer(item)
            TxHistoryItem.TransactionType.Approve -> mapApprove(item)
            TxHistoryItem.TransactionType.Swap -> mapSwap(item)
            TxHistoryItem.TransactionType.Deposit -> TransactionState.Custom(
                txHash = item.txHash,
                address = item.direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = item.timestampInMillis.toTimeFormat(),
                status = item.status.tiUiStatus(),
                title = TextReference.Str("Deposit"),
                subtitle = item.direction.extractAddress(),
                isIncoming = item.direction is TxHistoryItem.TransactionDirection.Incoming,
            )
            TxHistoryItem.TransactionType.Submit -> TransactionState.Custom(
                txHash = item.txHash,
                address = item.direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = item.timestampInMillis.toTimeFormat(),
                status = item.status.tiUiStatus(),
                title = TextReference.Str("Submit"),
                subtitle = item.direction.extractAddress(),
                isIncoming = item.direction is TxHistoryItem.TransactionDirection.Incoming,
            )
            TxHistoryItem.TransactionType.Supply -> TransactionState.Custom(
                txHash = item.txHash,
                address = item.direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = item.timestampInMillis.toTimeFormat(),
                status = item.status.tiUiStatus(),
                title = TextReference.Str("Supply"),
                subtitle = item.direction.extractAddress(),
                isIncoming = item.direction is TxHistoryItem.TransactionDirection.Incoming,
            )
            TxHistoryItem.TransactionType.Unoswap -> TransactionState.Custom(
                txHash = item.txHash,
                address = item.direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = item.timestampInMillis.toTimeFormat(),
                status = item.status.tiUiStatus(),
                title = TextReference.Str("Unoswap"),
                subtitle = item.direction.extractAddress(),
                isIncoming = item.direction is TxHistoryItem.TransactionDirection.Incoming,
            )
            TxHistoryItem.TransactionType.Withdraw -> TransactionState.Custom(
                txHash = item.txHash,
                address = item.direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = item.timestampInMillis.toTimeFormat(),
                status = item.status.tiUiStatus(),
                title = TextReference.Str("Withdraw"),
                subtitle = item.direction.extractAddress(),
                isIncoming = item.direction is TxHistoryItem.TransactionDirection.Incoming,
            )
            is TxHistoryItem.TransactionType.Custom -> TransactionState.Custom(
                txHash = item.txHash,
                address = item.direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = item.timestampInMillis.toTimeFormat(),
                status = item.status.tiUiStatus(),
                title = TextReference.Str(type.id),
                subtitle = item.direction.extractAddress(),
                isIncoming = item.direction is TxHistoryItem.TransactionDirection.Incoming,
            )
        }
    }

    private fun mapTransfer(item: TxHistoryItem): TransactionState {
        return when (item.direction) {
            is TxHistoryItem.TransactionDirection.Incoming -> TransactionState.Receive(
                txHash = item.txHash,
                address = item.direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = item.timestampInMillis.toTimeFormat(),
                status = item.status.tiUiStatus(),
            )
            is TxHistoryItem.TransactionDirection.Outgoing -> TransactionState.Send(
                txHash = item.txHash,
                address = item.direction.extractAddress(),
                amount = item.amount.toCryptoCurrencyFormat(),
                timestamp = item.timestampInMillis.toTimeFormat(),
                status = item.status.tiUiStatus(),
            )
        }
    }

    private fun mapApprove(item: TxHistoryItem): TransactionState {
        return TransactionState.Approve(
            txHash = item.txHash,
            address = item.direction.extractAddress(),
            amount = item.amount.toCryptoCurrencyFormat(),
            timestamp = item.timestampInMillis.toTimeFormat(),
            status = item.status.tiUiStatus(),
        )
    }
    private fun mapSwap(item: TxHistoryItem): TransactionState {
        return TransactionState.Swap(
            txHash = item.txHash,
            address = item.direction.extractAddress(),
            amount = item.amount.toCryptoCurrencyFormat(),
            timestamp = item.timestampInMillis.toTimeFormat(),
            status = item.status.tiUiStatus(),
        )
    }

    private fun BigDecimal.toCryptoCurrencyFormat(): String {
        return toFormattedCurrencyString(currency = symbol, decimals = decimals)
    }

    private fun TxHistoryItem.TransactionDirection.extractAddress(): TextReference = when (val addr = address) {
        TxHistoryItem.Address.Multiple -> TextReference.Res(R.string.transaction_history_multiple_addresses)
        is TxHistoryItem.Address.Single -> TextReference.Str(addr.rawAddress.toBriefAddressFormat())
    }

    private fun TxHistoryItem.TransactionStatus.tiUiStatus() = when (this) {
        TxHistoryItem.TransactionStatus.Confirmed -> TransactionState.Content.Status.Confirmed
        TxHistoryItem.TransactionStatus.Failed -> TransactionState.Content.Status.Failed
        TxHistoryItem.TransactionStatus.Unconfirmed -> TransactionState.Content.Status.Unconfirmed
    }

    private fun Long.toTimeFormat(): String {
        return DateTimeFormatters.formatTime(time = DateTime(this, DateTimeZone.getDefault()))
    }
}