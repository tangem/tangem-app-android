package com.tangem.features.send.impl.presentation.state.recipient

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.recipient.utils.RECENT_DEFAULT_COUNT
import com.tangem.features.send.impl.presentation.state.recipient.utils.RECENT_KEY_TAG
import com.tangem.features.send.impl.presentation.state.recipient.utils.emptyListState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

internal class SendRecipientHistoryListConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<List<TxHistoryItem>, ImmutableList<SendRecipientListContent>> {

    override fun convert(value: List<TxHistoryItem>): ImmutableList<SendRecipientListContent> {
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        return value.filterRecipients(cryptoCurrency).ifEmpty {
            emptyListState(RECENT_KEY_TAG, RECENT_DEFAULT_COUNT)
        }
    }

    private fun List<TxHistoryItem>.filterRecipients(cryptoCurrency: CryptoCurrency) = this.filter { item ->
        val isTransfer = item.type == TxHistoryItem.TransactionType.Transfer
        val isNotContract = item.interactionAddressType is TxHistoryItem.InteractionAddressType.User
        val isSingleAddress = if (item.isOutgoing) {
            item.destinationType is TxHistoryItem.DestinationType.Single
        } else {
            item.sourceType is TxHistoryItem.SourceType.Single
        }
        val notZero = !item.amount.isZero()
        isTransfer && isSingleAddress && isNotContract && item.isOutgoing && notZero
    }
        .take(RECENT_LIST_SIZE)
        .mapIndexed { index, tx ->
            SendRecipientListContent(
                id = "$RECENT_KEY_TAG$index",
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
        return amount.format { crypto(cryptoCurrency) }
    }

    private fun TxHistoryItem.extractTimestamp(): TextReference {
        val date = timestampInMillis.toDateFormatWithTodayYesterday(
            formatter = DateTimeFormatters.dateDDMMYYYY,
        )
        val time = timestampInMillis.toTimeFormat()
        return TextReference.Res(R.string.send_date_format, wrappedList(date, time))
    }

    companion object {
        private const val RECENT_LIST_SIZE = 10
    }
}