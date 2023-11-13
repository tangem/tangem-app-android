package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import androidx.paging.*
import com.tangem.common.Provider
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.state.TxHistoryState.TxHistoryItemState
import com.tangem.core.ui.utils.toDateFormat
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

internal class TokenDetailsTxHistoryItemFlowConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val symbol: String,
    private val decimals: Int,
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<Flow<PagingData<TxHistoryItem>>, TxHistoryState> {

    private val txHistoryItemConverter by lazy {
        TokenDetailsTxHistoryTransactionStateConverter(
            symbol = symbol,
            decimals = decimals,
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: Flow<PagingData<TxHistoryItem>>): TxHistoryState {
        val state = currentStateProvider()
        val txHistoryContent = if (state.txHistoryState is TxHistoryState.Content) {
            state.txHistoryState
        } else {
            TxHistoryState.Content(contentItems = MutableStateFlow(PagingData.empty()))
        }
        // FIXME: TxHistoryRepository should send loading transactions
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
                    state = txContent.copy(timestamp = txContent.timestamp.toLong().toTimeFormat()),
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
}