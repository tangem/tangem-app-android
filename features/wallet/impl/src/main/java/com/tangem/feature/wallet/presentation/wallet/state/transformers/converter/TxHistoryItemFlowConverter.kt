package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.paging.*
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.state.TxHistoryState.TxHistoryItemState
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.UUID

private val scope = CoroutineScope(Dispatchers.IO)

internal class TxHistoryItemFlowConverter(
    private val currentState: TxHistoryState,
    private val clickIntents: WalletClickIntents,
) : Converter<Flow<PagingData<TransactionState>>, TxHistoryState?> {

    override fun convert(value: Flow<PagingData<TransactionState>>): TxHistoryState {
        val txHistoryContent = currentState as? TxHistoryState.Content
            ?: TxHistoryState.Content(contentItems = MutableStateFlow(PagingData.empty()))

        // FIXME: TxHistoryRepository should send loading transactions
        // [REDACTED_JIRA]
        value
            .onEach { txHistoryStatePagingData ->
                txHistoryContent.contentItems.update {
                    txHistoryStatePagingData
                        .map<TransactionState, TxHistoryItemState> { item ->
                            TxHistoryItemState.Transaction(item)
                        }
                        .insertHeaderItem(
                            terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE,
                            item = TxHistoryItemState.Title(clickIntents::onExploreClick),
                        )
                        .insertGroupTitle()
                }
            }
            .cachedIn(scope)
            .launchIn(scope)

        return txHistoryContent
    }

    private fun PagingData<TxHistoryItemState>.insertGroupTitle(): PagingData<TxHistoryItemState> {
        return insertSeparators(terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE) { before, after ->
            // Use raw timestamp to get date

            // If [afterDate] is the first transaction in the flow, add the group title
            val afterDate = after.getTimestamp()?.toDateFormatWithTodayYesterday() ?: return@insertSeparators null
            if (before is TxHistoryItemState.Title) {
                return@insertSeparators TxHistoryItemState.GroupTitle(afterDate, itemKey = UUID.randomUUID().toString())
            }

            /*
             * If [beforeDate] is not equals to [afterDate], then [afterDate] is first transaction in
             * the new group
             */
            val beforeDate = before.getTimestamp()?.toDateFormatWithTodayYesterday() ?: return@insertSeparators null
            return@insertSeparators if (beforeDate != afterDate) {
                TxHistoryItemState.GroupTitle(afterDate, itemKey = UUID.randomUUID().toString())
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