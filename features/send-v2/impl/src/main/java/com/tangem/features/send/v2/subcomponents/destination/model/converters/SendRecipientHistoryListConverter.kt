package com.tangem.features.send.v2.subcomponents.destination.model.converters

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.RECENT_DEFAULT_COUNT
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.RECENT_KEY_TAG
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.emptyListState
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationRecipientListUM
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

internal class SendRecipientHistoryListConverter(
    private val cryptoCurrency: CryptoCurrency,
) : Converter<List<TxInfo>, ImmutableList<DestinationRecipientListUM>> {

    override fun convert(value: List<TxInfo>): ImmutableList<DestinationRecipientListUM> {
        return value.filterRecipients(cryptoCurrency).ifEmpty {
            emptyListState(RECENT_KEY_TAG, RECENT_DEFAULT_COUNT)
        }
    }

    private fun List<TxInfo>.filterRecipients(cryptoCurrency: CryptoCurrency) = this.filter { item ->
        val isTransfer = item.type == TxInfo.TransactionType.Transfer
        val isNotContract = item.interactionAddressType is TxInfo.InteractionAddressType.User
        val isSingleAddress = if (item.isOutgoing) {
            item.destinationType is TxInfo.DestinationType.Single
        } else {
            item.sourceType is TxInfo.SourceType.Single
        }
        val notZero = !item.amount.isZero()
        isTransfer && isSingleAddress && isNotContract && item.isOutgoing && notZero
    }
        .take(RECENT_LIST_SIZE)
        .mapIndexed { index, tx ->
            DestinationRecipientListUM(
                id = "${RECENT_KEY_TAG}$index",
                title = tx.extractAddress(),
                subtitle = stringReference(tx.getAmount(cryptoCurrency).trim()),
                timestamp = tx.extractTimestamp(),
                subtitleEndOffset = cryptoCurrency.symbol.length,
                subtitleIconRes = tx.extractIconRes(),
            )
        }.toPersistentList()

    private fun TxInfo.extractAddress(): TextReference = if (isOutgoing) {
        when (val destination = destinationType) {
            is TxInfo.DestinationType.Multiple -> resourceReference(
                R.string.transaction_history_multiple_addresses,
            )
            is TxInfo.DestinationType.Single -> stringReference(destination.addressType.address)
        }
    } else {
        when (val source = sourceType) {
            is TxInfo.SourceType.Multiple -> resourceReference(R.string.transaction_history_multiple_addresses)
            is TxInfo.SourceType.Single -> stringReference(source.address)
        }
    }

    private fun TxInfo.extractIconRes() = if (isOutgoing) {
        R.drawable.ic_arrow_up_24
    } else {
        R.drawable.ic_arrow_down_24
    }

    private fun TxInfo.getAmount(cryptoCurrency: CryptoCurrency): String {
        return amount.format { crypto(cryptoCurrency) }
    }

    private fun TxInfo.extractTimestamp(): TextReference {
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