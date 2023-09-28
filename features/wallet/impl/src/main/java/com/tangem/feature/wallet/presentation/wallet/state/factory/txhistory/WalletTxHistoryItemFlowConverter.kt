package com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory

import android.text.format.DateUtils
import androidx.paging.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.Provider
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.state.TxHistoryState.TxHistoryItemState
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isToday
import com.tangem.utils.extensions.isYesterday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/**
 * Convert from [Flow] of [TxHistoryItem] to [TxHistoryState]
 *
 * @property currentStateProvider current state provider
 * @property blockchain           blockchain of transactions history
 * @property clickIntents         screen click intents
 *
* [REDACTED_AUTHOR]
 */
internal class WalletTxHistoryItemFlowConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val blockchain: Blockchain,
    private val clickIntents: WalletClickIntents,
) : Converter<Flow<PagingData<TxHistoryItem>>, TxHistoryState?> {

    private val txHistoryItemConverter by lazy {
        WalletTxHistoryTransactionStateConverter(
            symbol = blockchain.currency,
            decimals = blockchain.decimals(),
            isBalanceHiddenProvider = isBalanceHiddenProvider,
        )
    }

    override fun convert(value: Flow<PagingData<TxHistoryItem>>): TxHistoryState? {
        val state = currentStateProvider() as? WalletSingleCurrencyState ?: return null
        val txHistoryContent = state.txHistoryState as? TxHistoryState.Content ?: return state.txHistoryState
// [REDACTED_TODO_COMMENT]
// [REDACTED_JIRA]
        value
            .onEach { txHistoryStatePagingData ->
                txHistoryContent.contentItems.update {
                    txHistoryStatePagingData
                        .map<TxHistoryItem, TxHistoryItemState> { item ->
                            // [createTransactionState] returns timestamp without formatting
                            TxHistoryItemState.Transaction(state = createTransactionState(item))
                        }
                        .insertHeaderItem(
                            terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE,
                            item = TxHistoryItemState.Title(clickIntents::onExploreClick),
                        )
                        .insertGroupTitle() // method uses the raw timestamp
                        .formatTransactionsTimestamp() // method formats the timestamp
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        return txHistoryContent
    }

    private fun createTransactionState(item: TxHistoryItem): TransactionState {
        return txHistoryItemConverter.convert(value = item)
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
                val txContent = txHistoryItemState.state as TransactionState.Content
                txHistoryItemState.copy(
                    state = txContent.copySealed(
                        timestamp = txContent.timestamp.toTimeFormat(),
                    ),
                )
            } else {
                txHistoryItemState
            }
        }
    }

    private fun TxHistoryItemState?.getTimestamp(): Long? {
        return if (this is TxHistoryItemState.Transaction && this.state is TransactionState.Content) {
            val txContent = this.state as TransactionState.Content
            requireNotNull(txContent.timestamp.toLongOrNull()) { "Timestamp must be Long type" }
        } else {
            null
        }
    }

    /**
     * If [this] timestamp is today or yesterday, returns relative date,
     * otherwise returns formatting date.
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
            DateTimeFormatters.formatDate(date = localDate)
        }
    }

    private fun String.toTimeFormat(): String {
        return DateTimeFormatters.formatTime(time = DateTime(this.toLong(), DateTimeZone.getDefault()))
    }
}
