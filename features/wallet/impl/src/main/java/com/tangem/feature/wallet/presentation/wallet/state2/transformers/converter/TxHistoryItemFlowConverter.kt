package com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter

import androidx.paging.*
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.state.TxHistoryState.TxHistoryItemState
import com.tangem.core.ui.utils.toDateFormat
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.UUID

private val scope = CoroutineScope(Dispatchers.IO)

internal class TxHistoryItemFlowConverter(
    private val userWallet: UserWallet,
    private val currentState: TxHistoryState,
    private val clickIntents: WalletClickIntentsV2,
) : Converter<Flow<PagingData<TxHistoryItem>>, TxHistoryState?> {

    private val txHistoryItemConverter by lazy {
        val blockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()
        TxHistoryItemStateConverter(
            symbol = blockchain.currency,
            decimals = blockchain.decimals(),
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: Flow<PagingData<TxHistoryItem>>): TxHistoryState {
        val txHistoryContent = currentState as? TxHistoryState.Content
            ?: TxHistoryState.Content(contentItems = MutableStateFlow(PagingData.empty()))

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
            .cachedIn(scope)
            .launchIn(scope)

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
                return@insertSeparators TxHistoryItemState.GroupTitle(afterDate, itemKey = UUID.randomUUID().toString())
            }

            /*
             * If [beforeDate] is not equals to [afterDate], then [afterDate] is first transaction in
             * the new group
             */
            val beforeDate = before.getTimestamp()?.toDateFormat() ?: return@insertSeparators null
            return@insertSeparators if (beforeDate != afterDate) {
                TxHistoryItemState.GroupTitle(afterDate, itemKey = UUID.randomUUID().toString())
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