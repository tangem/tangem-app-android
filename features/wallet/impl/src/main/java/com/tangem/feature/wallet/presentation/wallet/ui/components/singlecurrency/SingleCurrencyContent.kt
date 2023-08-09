package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionBlock
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionsBlockState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.ui.decorations.walletContentItemDecoration

/**
 * LazyList extension for transactions history [WalletTxHistoryState]
 *
 * @param state          state
 * @param txHistoryItems transactions
 * @param modifier       modifier
 */
internal fun LazyListScope.txHistoryItems(
    state: WalletTxHistoryState,
    txHistoryItems: LazyPagingItems<WalletTxHistoryState.TxHistoryItemState>?,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletTxHistoryState.ContentState -> {
            contentItems(
                txHistoryItems = requireNotNull(txHistoryItems),
                modifier = modifier,
            )
        }
        is WalletTxHistoryState.Empty -> {
            nonContentItem(
                state = EmptyTransactionsBlockState.Empty(onClick = state.onBuyClick),
                modifier = modifier,
            )
        }
        is WalletTxHistoryState.Error -> {
            nonContentItem(
                state = EmptyTransactionsBlockState.FailedToLoad(onClick = state.onReloadClick),
                modifier = modifier,
            )
        }
        is WalletTxHistoryState.NotSupported -> {
            nonContentItem(
                state = EmptyTransactionsBlockState.NotImplemented(onClick = state.onExploreClick),
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.contentItems(
    txHistoryItems: LazyPagingItems<WalletTxHistoryState.TxHistoryItemState>,
    modifier: Modifier = Modifier,
) {
    itemsIndexed(
        items = txHistoryItems,
        key = { index, _ -> index },
        itemContent = { index, item ->
            if (item == null) return@itemsIndexed

            SingleCurrencyContentItem(
                state = item,
                modifier = modifier
                    .animateItemPlacement()
                    .walletContentItemDecoration(
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
