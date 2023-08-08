package com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory

import android.text.format.DateUtils
import androidx.paging.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.domain.txhistory.model.TxHistoryItem
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTxHistoryState.TxHistoryItemState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isToday
import com.tangem.utils.extensions.isYesterday
import com.tangem.utils.toBriefAddressFormat
import com.tangem.utils.toFormattedCurrencyString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatterBuilder
import java.math.BigDecimal
import java.util.Locale

/**
 * Convert from [Flow] of [TxHistoryItem] to [WalletTxHistoryState]
 *
 * @property blockchain   blockchain of transactions history
 * @property clickIntents screen click intents
 *
[REDACTED_AUTHOR]
 */
internal class WalletTxHistoryItemFlowConverter(
    private val blockchain: Blockchain,
    private val clickIntents: WalletClickIntents,
) : Converter<Flow<PagingData<TxHistoryItem>>, WalletTxHistoryState> {

    /** Example, 2 Aug, 2023 */
    private val dateFormatter by lazy {
        DateTimeFormatterBuilder()
            .appendDayOfMonth(1)
            .appendLiteral(' ')
            .appendMonthOfYearShortText()
            .appendLiteral(", ")
            .appendYear(4, 4)
            .toFormatter()
            .withLocale(Locale.getDefault())
    }

    /** Example, 13:35 */
    private val timeFormatter by lazy {
        DateTimeFormatterBuilder()
            .appendHourOfDay(1)
            .appendLiteral(':')
            .appendMinuteOfHour(2)
            .toFormatter()
            .withLocale(Locale.getDefault())
    }

    override fun convert(value: Flow<PagingData<TxHistoryItem>>): WalletTxHistoryState {
        return WalletTxHistoryState.Content(
            items = value
                .map { pagingData ->
                    pagingData
                        .map<TxHistoryItem, TxHistoryItemState> { item ->
                            // [createTransactionState] returns timestamp without formatting
                            TxHistoryItemState.Transaction(state = createTransactionState(item))
                        }
                        .insertHeaderItem(
                            terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE,
                            item = TxHistoryItemState.Title(onExploreClick = clickIntents::onExploreClick),
                        )
                        .insertGroupTitle() // method uses the raw timestamp
                        .formatTransactionsTimestamp() // method formats the timestamp
                },
        )
    }

    private fun createTransactionState(item: TxHistoryItem): TransactionState {
        return when (item.type) {
            TxHistoryItem.TransactionType.Transfer -> {
                when (val direction = item.direction) {
                    is TxHistoryItem.TransactionDirection.Incoming -> {
                        createIncomingTransferTransaction(item, direction, blockchain)
                    }
                    is TxHistoryItem.TransactionDirection.Outgoing -> {
                        createOutgoingTransferTransaction(item, direction, blockchain)
                    }
                }
            }
        }
    }

    private fun createIncomingTransferTransaction(
        item: TxHistoryItem,
        direction: TxHistoryItem.TransactionDirection.Incoming,
        blockchain: Blockchain,
    ): TransactionState {
        return when (item.status) {
            TxHistoryItem.TxStatus.Confirmed -> TransactionState.Receive(
                address = direction.from.toBriefAddressFormat(),
                amount = item.amount.toCryptoCurrencyFormat(blockchain = blockchain),
                timestamp = item.getRawTimestamp(),
            )
            TxHistoryItem.TxStatus.Unconfirmed -> TransactionState.Receiving(
                address = direction.from.toBriefAddressFormat(),
                amount = item.amount.toCryptoCurrencyFormat(blockchain = blockchain),
                timestamp = item.getRawTimestamp(),
            )
        }
    }

    private fun createOutgoingTransferTransaction(
        item: TxHistoryItem,
        direction: TxHistoryItem.TransactionDirection.Outgoing,
        blockchain: Blockchain,
    ): TransactionState {
        return when (item.status) {
            TxHistoryItem.TxStatus.Confirmed -> TransactionState.Send(
                address = direction.to.toBriefAddressFormat(),
                amount = item.amount.toCryptoCurrencyFormat(blockchain = blockchain),
                timestamp = item.getRawTimestamp(),
            )
            TxHistoryItem.TxStatus.Unconfirmed -> TransactionState.Sending(
                address = direction.to.toBriefAddressFormat(),
                amount = item.amount.toCryptoCurrencyFormat(blockchain = blockchain),
                timestamp = item.getRawTimestamp(),
            )
        }
    }

    private fun BigDecimal.toCryptoCurrencyFormat(blockchain: Blockchain): String {
        return toFormattedCurrencyString(currency = blockchain.currency, decimals = blockchain.decimals())
    }

    private fun PagingData<TxHistoryItemState>.insertGroupTitle(): PagingData<TxHistoryItemState> {
        return insertSeparators(terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE) { before, after ->
            // Use raw timestamp to get date

            // If [afterDate] is the first transaction in the flow, add the group title
            val afterDate = after.getTimestamp()?.toDateFormat() ?: return@insertSeparators null
            if (before is TxHistoryItemState.Title) {
                return@insertSeparators TxHistoryItemState.GroupTitle(afterDate)
            }

            /*
             * If [beforeDate] is not equals to [afterDate], then [afterDate] is first transaction in
             * the new group
             */
            val beforeDate = before.getTimestamp()?.toDateFormat() ?: return@insertSeparators null
            return@insertSeparators if (beforeDate != afterDate) {
                TxHistoryItemState.GroupTitle(afterDate)
            } else {
                null
            }
        }
    }

    /**
     * Map the [PagingData] to format the [TxHistoryItemState] timestamp
     */
    private fun PagingData<TxHistoryItemState>.formatTransactionsTimestamp(): PagingData<TxHistoryItemState> {
        return map { txHistoryItemState ->
            if (txHistoryItemState is TxHistoryItemState.Transaction &&
                txHistoryItemState.state is TransactionState.Content
            ) {
                txHistoryItemState.copy(
                    state = txHistoryItemState.state.copySealed(
                        timestamp = txHistoryItemState.state.timestamp.toTimeFormat(),
                    ),
                )
            } else {
                txHistoryItemState
            }
        }
    }

    /**
     * Get timestamp without formatting.
     * It's life hack that help us to add transaction's group title to flow.
     *
     * @see [convert]
     */
    private fun TxHistoryItem.getRawTimestamp() = this.timestamp.toString()

    private fun TxHistoryItemState?.getTimestamp(): Long? {
        return if (this is TxHistoryItemState.Transaction && this.state is TransactionState.Content) {
            requireNotNull(this.state.timestamp.toLongOrNull()) { "Timestamp must be Long type" }
        } else {
            null
        }
    }

    /**
     * If [this] timestamp is today or yesterday, returns relative date,
     * otherwise returns formatting date by [dateFormatter]
     */
    private fun Long.toDateFormat(): String {
        val localDate = DateTime(this, DateTimeZone.getDefault())
        return if (localDate.isToday() || localDate.isYesterday()) {
            DateUtils.getRelativeTimeSpanString(
                this,
                DateTime.now().millis,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()
        } else {
            dateFormatter.print(localDate)
        }
    }

    private fun String.toTimeFormat(): String {
        return timeFormatter.print(
            DateTime(this.toLong(), DateTimeZone.getDefault()),
        )
    }
}