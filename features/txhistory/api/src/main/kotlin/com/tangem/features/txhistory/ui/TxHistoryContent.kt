package com.tangem.features.txhistory.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.transactions.TransactionItem
import com.tangem.core.ui.components.transactions.TxHistoryDateHeader
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionBlock
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionsBlockState
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryItemsUM.TxHistoryItemUM

private val LoadingTitleShimmerWidth = 52.dp
private val LoadingPrimaryShimmerWidth = 110.dp
private val LoadingSecondaryShimmerWidth = 52.dp
private val LoadingEndTopShimmerWidth = 107.dp
private val LoadingEndBottomShimmerWidth = 52.dp

private const val LOADING_TRANSACTION_MIN_ALPHA = 0.1f
private const val LOAD_MORE_BUFFER = 20

fun LazyListScope.txHistoryItems(listState: LazyListState, state: TxHistoryItemsUM) {
    when (state) {
        is TxHistoryItemsUM.Content -> contentItems(listState, state)
        is TxHistoryItemsUM.Empty -> emptyItem(state)
        is TxHistoryItemsUM.Error -> errorItem(state)
        is TxHistoryItemsUM.Loading -> loadingItems(state)
        is TxHistoryItemsUM.NotSupported -> notSupportedItem(state)
    }
}

private fun LazyListScope.contentItems(listState: LazyListState, state: TxHistoryItemsUM.Content) {
    items(
        items = state.items,
        key = { item ->
            when (item) {
                is TxHistoryItemUM.GroupTitle -> "group_title:${item.itemKey}"
                is TxHistoryItemUM.Transaction -> "tx:${item.state.txHash}:${item.state.hashCode()}"
            }
        },
        contentType = { item -> item::class.java },
    ) { item ->
        when (item) {
            is TxHistoryItemUM.GroupTitle -> TxHistoryDateHeader(title = item.title)
            is TxHistoryItemUM.Transaction -> TransactionItem(
                state = item.state,
                isBalanceHidden = state.isBalanceHidden,
            )
        }
    }
    item(key = "tx_history_load_more", contentType = "tx_history_load_more") {
        TxHistoryLoadMoreFooter(
            listState = listState,
            isLoadingMore = state.isLoadingMore,
            onLoadMore = state.loadMore,
        )
    }
}

@Composable
private fun TxHistoryLoadMoreFooter(
    listState: LazyListState,
    isLoadingMore: Boolean,
    onLoadMore: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_MORE_BUFFER,
        onLoadMore = onLoadMore,
    )
    if (isLoadingMore) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = TangemTheme.dimens2.x4),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(TangemTheme.dimens2.x6),
                color = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
                strokeWidth = TangemTheme.dimens2.x0_5,
            )
        }
    }
}

private fun LazyListScope.emptyItem(state: TxHistoryItemsUM.Empty) {
    item(key = "tx_history_empty", contentType = "tx_history_empty") {
        TxHistoryEmptyBlock(state = state)
    }
}

private fun LazyListScope.errorItem(state: TxHistoryItemsUM.Error) {
    item(key = "tx_history_error", contentType = "tx_history_error") {
        TxHistoryErrorBlock(state = state)
    }
}

private fun LazyListScope.loadingItems(state: TxHistoryItemsUM.Loading) {
    item(key = "tx_history_loading", contentType = "tx_history_loading") {
        TxHistoryLoadingBlock(state = state)
    }
}

private fun LazyListScope.notSupportedItem(state: TxHistoryItemsUM.NotSupported) {
    if (state.pendingTransactions.isNotEmpty()) {
        item(key = "tx_history_pending_header", contentType = "tx_history_pending_header") {
            TxHistoryDateHeader(title = stringResourceSafe(R.string.transaction_history_pending))
        }
        items(
            items = state.pendingTransactions,
            key = { item -> "pending_tx:${item.txHash}" },
            contentType = { TransactionItemUM::class.java },
        ) { item ->
            TransactionItem(state = item, isBalanceHidden = state.isBalanceHidden)
        }
    }
    item(key = "tx_history_not_supported", contentType = "tx_history_not_supported") {
        TxHistoryNotSupportedBlock(state = state)
    }
}

@Composable
private fun TxHistoryEmptyBlock(state: TxHistoryItemsUM.Empty, modifier: Modifier = Modifier) {
    EmptyTransactionBlock(
        state = EmptyTransactionsBlockState.Empty(
            onExplore = state.onExploreClick,
            exploreIconResId = R.drawable.ic_compass_24,
        ),
        modifier = modifier,
    )
}

@Composable
private fun TxHistoryErrorBlock(state: TxHistoryItemsUM.Error, modifier: Modifier = Modifier) {
    EmptyTransactionBlock(
        state = EmptyTransactionsBlockState.FailedToLoad(
            onReload = state.onReloadClick,
            onExplore = state.onExploreClick,
            reloadIconResId = R.drawable.ic_refresh_24,
            exploreIconResId = R.drawable.ic_compass_24,
        ),
        modifier = modifier,
    )
}

@Composable
private fun TxHistoryLoadingBlock(state: TxHistoryItemsUM.Loading, modifier: Modifier = Modifier) {
    val lastIndex = state.items.lastIndex
    Column(modifier = modifier.fillMaxWidth()) {
        TxHistoryLoadingDateHeader()
        state.items.forEachIndexed { index, _ ->
            val fraction = if (lastIndex <= 0) 0f else index.toFloat() / lastIndex
            val alpha = lerp(start = 1f, stop = LOADING_TRANSACTION_MIN_ALPHA, fraction = fraction)
            TxHistoryLoadingTransaction(modifier = Modifier.alpha(alpha))
        }
    }
}

@Composable
private fun TxHistoryLoadingDateHeader(modifier: Modifier = Modifier) {
    RectangleShimmer(
        modifier = modifier
            .padding(
                top = TangemTheme.dimens2.x6,
                bottom = TangemTheme.dimens2.x3,
                start = TangemTheme.dimens2.x4,
            )
            .size(width = LoadingTitleShimmerWidth, height = TangemTheme.dimens2.x4),
        radius = TangemTheme.dimens2.x2,
    )
}

@Composable
private fun TxHistoryLoadingTransaction(modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = TangemTheme.dimens2.x4,
            vertical = TangemTheme.dimens2.x3,
        ),
        content = {
            CircleShimmer(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.HEAD)
                    .padding(end = TangemTheme.dimens2.x3)
                    .size(TangemTheme.dimens2.x10),
            )
            RectangleShimmer(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.START_TOP)
                    .size(width = LoadingPrimaryShimmerWidth, height = TangemTheme.dimens2.x5),
                radius = TangemTheme.dimens2.x2,
            )
            RectangleShimmer(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.START_BOTTOM)
                    .size(width = LoadingSecondaryShimmerWidth, height = TangemTheme.dimens2.x4),
                radius = TangemTheme.dimens2.x2,
            )
            RectangleShimmer(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.END_TOP)
                    .size(width = LoadingEndTopShimmerWidth, height = TangemTheme.dimens2.x5),
                radius = TangemTheme.dimens2.x2,
            )
            RectangleShimmer(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.END_BOTTOM)
                    .size(width = LoadingEndBottomShimmerWidth, height = TangemTheme.dimens2.x4),
                radius = TangemTheme.dimens2.x2,
            )
        },
    )
}

@Composable
private fun TxHistoryNotSupportedBlock(state: TxHistoryItemsUM.NotSupported, modifier: Modifier = Modifier) {
    EmptyTransactionBlock(
        state = EmptyTransactionsBlockState.NotImplemented(
            onExplore = state.onExploreClick,
            exploreIconResId = R.drawable.ic_compass_24,
        ),
        modifier = modifier,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TxHistoryLoadingBlock_Preview() {
    TangemThemePreviewRedesign {
        TxHistoryLoadingBlock(
            state = TxHistoryItemsUM.Loading(
                isBalanceHidden = false,
                onExploreClick = {},
            ),
        )
    }
}
// endregion Preview