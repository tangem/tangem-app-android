package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionBlock
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionsBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme

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
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TxHistoryState.ContentState -> {
            contentItems(
                txHistoryItems = requireNotNull(txHistoryItems),
                modifier = modifier,
            )
        }
        is TxHistoryState.Empty -> {
            nonContentItem(
                state = EmptyTransactionsBlockState.Empty(onClick = state.onBuyClick),
                modifier = modifier,
            )
        }
        is TxHistoryState.Error -> {
            nonContentItem(
                state = EmptyTransactionsBlockState.FailedToLoad(onClick = state.onReloadClick),
                modifier = modifier,
            )
        }
        is TxHistoryState.NotSupported -> {
            nonContentItem(
                state = EmptyTransactionsBlockState.NotImplemented(onClick = state.onExploreClick),
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.contentItems(
    txHistoryItems: LazyPagingItems<TxHistoryState.TxHistoryItemState>,
    modifier: Modifier = Modifier,
) {
    itemsIndexed(
        items = txHistoryItems,
        key = { index, _ -> index },
        itemContent = { index, item ->
            if (item == null) return@itemsIndexed

            TxHistoryListItem(
                state = item,
                modifier = modifier
                    .animateItemPlacement()
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = txHistoryItems.itemSnapshotList.lastIndex,
                    ),
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.nonContentItem(state: EmptyTransactionsBlockState, modifier: Modifier = Modifier) {
    item {
        EmptyTransactionBlock(
            state = state,
            modifier = modifier
                .animateItemPlacement()
                .padding(horizontal = TangemTheme.dimens.spacing16, vertical = TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
        )
    }
}