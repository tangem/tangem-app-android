package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import androidx.paging.*
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.state.TxHistoryState.TxHistoryItemState
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.UUID

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
                        .insertGroupTitle()
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
            val afterDate = after.getTimestamp()?.toDateFormatWithTodayYesterday() ?: return@insertSeparators null
            if (before is TxHistoryItemState.Title) {
                return@insertSeparators TxHistoryItemState.GroupTitle(
                    title = afterDate,
                    itemKey = UUID.randomUUID().toString(),
                )
            }

            /*
             * If [beforeDate] is not equals to [afterDate], then [afterDate] is first transaction in
             * the new group
             */
            val beforeDate = before.getTimestamp()?.toDateFormatWithTodayYesterday() ?: return@insertSeparators null
            return@insertSeparators if (beforeDate != afterDate) {
                TxHistoryItemState.GroupTitle(
                    title = afterDate,
                    itemKey = UUID.randomUUID().toString(),
                )
            } else {
                null
            }
        }
    }

    private fun TxHistoryItemState?.getTimestamp(): Long? {
        return if (this is TxHistoryItemState.Transaction && this.state is TransactionState.Content) {
            val txContent = this.state as TransactionState.Content
            txContent.timestamp
        } else {
            null
        }
    }
}