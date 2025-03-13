package com.tangem.features.txhistory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.transactions.PendingTxsBlock
import com.tangem.core.ui.components.transactions.Transaction
import com.tangem.core.ui.components.transactions.TxHistoryTitle
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionBlock
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionsBlockState
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.txhistory.entity.TxHistoryUM

private const val LOAD_ITEMS_BUFFER = 20

internal fun LazyListScope.txHistoryItems(listState: LazyListState, state: TxHistoryUM) {
    when (state) {
        is TxHistoryUM.Content -> contentItems(listState, state)
        is TxHistoryUM.Empty -> nonContentItem(state = EmptyTransactionsBlockState.Empty(state.onExploreClick))
        is TxHistoryUM.Error -> nonContentItem(
            state = EmptyTransactionsBlockState.FailedToLoad(
                onReload = state.onReloadClick,
                onExplore = state.onExploreClick,
            ),
        )
        is TxHistoryUM.Loading -> loadingItems(state)
        is TxHistoryUM.NotSupported -> {
            if (state.pendingTransactions.isNotEmpty()) {
                item(key = "PendingTxsBlock", contentType = "PendingTxsBlock") {
                    PendingTxsBlock(pendingTxs = state.pendingTransactions, isBalanceHidden = state.isBalanceHidden)
                }
            }

            nonContentItem(
                state = EmptyTransactionsBlockState.NotImplemented(onExplore = state.onExploreClick),
            )
        }
    }
}

private fun LazyListScope.nonContentItem(state: EmptyTransactionsBlockState, modifier: Modifier = Modifier) {
    item(key = state::class.java, contentType = state::class.java) {
        EmptyTransactionBlock(
            state = state,
            modifier = modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .padding(horizontal = TangemTheme.dimens.spacing16, vertical = TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
        )
    }
}

private fun LazyListScope.loadingItems(state: TxHistoryUM.Loading) {
    itemsIndexed(
        items = state.items,
        key = { _, item ->
            when (item) {
                is TxHistoryUM.TxHistoryItemUM.GroupTitle -> item.itemKey
                is TxHistoryUM.TxHistoryItemUM.Title -> item.onExploreClick.hashCode()
                is TxHistoryUM.TxHistoryItemUM.Transaction ->
                    item.state.txHash + (item.state as? TransactionState.Content)?.hashCode()
            }
        },
        contentType = { _, item -> item::class.java },
        itemContent = { index, item ->
            TxHistoryListItem(
                state = item,
                isBalanceHidden = true,
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = state.items.lastIndex,
                ),
            )
        },
    )
}

private fun LazyListScope.contentItems(listState: LazyListState, state: TxHistoryUM.Content) {
    itemsIndexed(
        items = state.items,
        key = { _, item ->
            when (item) {
                is TxHistoryUM.TxHistoryItemUM.GroupTitle -> item.itemKey
                is TxHistoryUM.TxHistoryItemUM.Title -> item.onExploreClick.hashCode()
                is TxHistoryUM.TxHistoryItemUM.Transaction ->
                    item.state.txHash + (item.state as? TransactionState.Content)?.hashCode()
            }
        },
        contentType = { _, item -> item::class.java },
        itemContent = { index, item ->
            TxHistoryListItem(
                state = item,
                isBalanceHidden = state.isBalanceHidden,
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = state.items.lastIndex,
                ),
            )
        },
    )
    item {
        InfiniteListHandler(
            listState = listState,
            buffer = LOAD_ITEMS_BUFFER,
            onLoadMore = state.loadMore,
        )
    }
}

@Composable
internal fun TxHistoryListItem(
    state: TxHistoryUM.TxHistoryItemUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TxHistoryUM.TxHistoryItemUM.GroupTitle -> {
            TxHistoryGroupTitle(config = state, modifier = modifier)
        }
        is TxHistoryUM.TxHistoryItemUM.Title -> {
            TxHistoryTitle(onExploreClick = state.onExploreClick, modifier = modifier)
        }
        is TxHistoryUM.TxHistoryItemUM.Transaction -> {
            Transaction(
                state = state.state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TxHistoryGroupTitle(config: TxHistoryUM.TxHistoryItemUM.GroupTitle, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .padding(
                vertical = TangemTheme.dimens.spacing8,
                horizontal = TangemTheme.dimens.spacing12,
            )
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size24),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = config.title,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
        )
    }
}