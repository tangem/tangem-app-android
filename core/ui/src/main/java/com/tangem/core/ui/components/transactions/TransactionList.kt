package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.util.fastForEach
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionBlock
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionsBlockState
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * LazyList extension for transactions history [TxHistoryState]
 *
 * @param state          state
 * @param txHistoryItems transactions
 * @param modifier       modifier
 */
fun LazyListScope.txHistoryItems(
    state: TxHistoryState,
    txHistoryItems: LazyPagingItems<TxHistoryState.TxHistoryItemState>?,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TxHistoryState.Content -> {
            contentItems(
                txHistoryItems = requireNotNull(txHistoryItems),
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
        }
        is TxHistoryState.Empty -> {
            nonContentItem(state = EmptyTransactionsBlockState.Empty(state.onExploreClick), modifier = modifier)
        }
        is TxHistoryState.Error -> {
            nonContentItem(
                state = EmptyTransactionsBlockState.FailedToLoad(
                    onReload = state.onReloadClick,
                    onExplore = state.onExploreClick,
                ),
                modifier = modifier,
            )
        }
        is TxHistoryState.NotSupported -> {
            if (state.pendingTransactions.isNotEmpty()) {
                item(key = "PendingTxsBlock", contentType = "PendingTxsBlock") {
                    PendingTxsBlock(pendingTxs = state.pendingTransactions, isBalanceHidden = isBalanceHidden)
                }
            }

            nonContentItem(
                state = EmptyTransactionsBlockState.NotImplemented(onExplore = state.onExploreClick),
                modifier = modifier,
            )
        }
    }
}

private fun LazyListScope.contentItems(
    txHistoryItems: LazyPagingItems<TxHistoryState.TxHistoryItemState>,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    items(
        count = txHistoryItems.itemCount,
        key = txHistoryItems.itemKey { item ->
            when (item) {
                is TxHistoryState.TxHistoryItemState.GroupTitle -> item.itemKey
                is TxHistoryState.TxHistoryItemState.Title -> item.onExploreClick.hashCode()
                is TxHistoryState.TxHistoryItemState.Transaction ->
                    item.state.txHash +
                        ((item.state as? TransactionState.Content)?.hashCode() ?: "")
            }
        },
        contentType = txHistoryItems.itemContentType { it::class.java },
        itemContent = { index ->
            txHistoryItems[index]?.let { item ->
                TxHistoryListItem(
                    state = item,
                    isBalanceHidden = isBalanceHidden,
                    modifier = modifier
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            lastIndex = txHistoryItems.itemSnapshotList.lastIndex,
                        ),
                )
            }
        },
    )
}

@Composable
private fun PendingTxsBlock(pendingTxs: ImmutableList<TransactionState>, isBalanceHidden: Boolean) {
    Column(
        modifier = Modifier
            .padding(top = TangemTheme.dimens.spacing12)
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .background(color = TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
        horizontalAlignment = Alignment.Start,
    ) {
        pendingTxs.fastForEach { Transaction(state = it, isBalanceHidden = isBalanceHidden) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.nonContentItem(state: EmptyTransactionsBlockState, modifier: Modifier = Modifier) {
    item(key = state::class.java, contentType = state::class.java) {
        EmptyTransactionBlock(
            state = state,
            modifier = modifier
                .animateItemPlacement()
                .padding(horizontal = TangemTheme.dimens.spacing16, vertical = TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
        )
    }
}