package com.tangem.features.send.impl.presentation.state.recipient

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toDateFormat
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.Provider
import com.tangem.utils.toFormattedCurrencyString
import kotlinx.collections.immutable.toPersistentList

internal class SendRecipientListConverter(
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) {

    fun convert(wallets: List<AvailableWallet?>, txHistory: List<TxHistoryItem>): SendUiState {
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        val state = currentStateProvider()
        val recipientState = state.recipientState ?: return state

        return state.copy(
            recipientState = recipientState.copy(
                wallets = wallets.filterWallets(),
                recent = txHistory.filterRecipients(cryptoCurrency),
            ),
        )
    }

    private fun List<AvailableWallet?>.filterWallets() = this.filterNotNull()
        .groupBy { item -> item.name }
        .values.map {
            it.mapIndexed { index, item ->
                val name = if (it.size > 1) {
                    "${item.name} ${index.inc()}"
                } else {
                    item.name
                }
                SendRecipientListContent(
                    id = item.address,
                    title = TextReference.Str(item.address),
                    subtitle = TextReference.Str(name),
                )
            }
        }
        .flatten()
        .toPersistentList()

    private fun List<TxHistoryItem>.filterRecipients(cryptoCurrency: CryptoCurrency) = this.filter { item ->
        val isTransfer = item.type == TxHistoryItem.TransactionType.Transfer
        val isNotContract = item.interactionAddressType is TxHistoryItem.InteractionAddressType.User
        val isSingleAddress = if (item.isOutgoing) {
            item.destinationType is TxHistoryItem.DestinationType.Single
        } else {
            item.sourceType is TxHistoryItem.SourceType.Single
        }
        isTransfer && isSingleAddress && isNotContract
    }
        .take(RECENT_LIST_SIZE)
        .map { tx ->
            SendRecipientListContent(
                id = tx.txHash,
                title = tx.extractAddress(),
                subtitle = stringReference(tx.getAmount(cryptoCurrency).trim()),
                timestamp = tx.extractTimestamp(),
                subtitleEndOffset = cryptoCurrency.symbol.length,
                subtitleIconRes = tx.extractIconRes(),
            )
        }.toPersistentList()

    private fun TxHistoryItem.extractAddress(): TextReference = if (isOutgoing) {
        when (val destination = destinationType) {
            is TxHistoryItem.DestinationType.Multiple -> TextReference.Res(
                R.string.transaction_history_multiple_addresses,
            )
            is TxHistoryItem.DestinationType.Single -> TextReference.Str(destination.addressType.address)
        }
    } else {
        when (val source = sourceType) {
            is TxHistoryItem.SourceType.Multiple -> TextReference.Res(R.string.transaction_history_multiple_addresses)
            is TxHistoryItem.SourceType.Single -> TextReference.Str(source.address)
        }
    }

    private fun TxHistoryItem.extractIconRes() = if (isOutgoing) {
        R.drawable.ic_arrow_up_24
    } else {
        R.drawable.ic_arrow_down_24
    }

    private fun TxHistoryItem.getAmount(cryptoCurrency: CryptoCurrency): String {
        return amount.toFormattedCurrencyString(
            currency = cryptoCurrency.symbol,
            decimals = cryptoCurrency.decimals,
        )
    }

    private fun TxHistoryItem.extractTimestamp(): TextReference {
        val date = timestampInMillis.toDateFormat(
            formatter = DateTimeFormatters.dateDDMMYYYY,
        )
        val time = timestampInMillis.toTimeFormat()
        return TextReference.Res(R.string.send_date_format, wrappedList(date, time))
    }

    companion object {
        private const val RECENT_LIST_SIZE = 10
    }
}