package com.tangem.features.txhistory.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionBlock
import com.tangem.core.ui.components.transactions.empty.EmptyTransactionsBlockState
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.txhistory.entity.TxHistoryUM

private val LoadingTitleShimmerWidth = 52.dp
private val LoadingPrimaryShimmerWidth = 110.dp
private val LoadingSecondaryShimmerWidth = 52.dp
private val LoadingEndTopShimmerWidth = 107.dp
private val LoadingEndBottomShimmerWidth = 52.dp

private const val LOADING_TRANSACTION_MIN_ALPHA = 0.1f

fun LazyListScope.txHistoryItems(listState: LazyListState, state: TxHistoryUM) {
    when (state) {
        is TxHistoryUM.Content -> contentItems(listState, state)
        is TxHistoryUM.Empty -> emptyItem(state)
        is TxHistoryUM.Error -> errorItem(state)
        is TxHistoryUM.Loading -> loadingItems(state)
        is TxHistoryUM.NotSupported -> notSupportedItem(state)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun LazyListScope.contentItems(listState: LazyListState, state: TxHistoryUM.Content) {
    item(key = "tx_history_content", contentType = "tx_history_content") {
        TxHistoryContentBlock(state = state)
    }
}

private fun LazyListScope.emptyItem(state: TxHistoryUM.Empty) {
    item(key = "tx_history_empty", contentType = "tx_history_empty") {
        TxHistoryEmptyBlock(state = state)
    }
}

private fun LazyListScope.errorItem(state: TxHistoryUM.Error) {
    item(key = "tx_history_error", contentType = "tx_history_error") {
        TxHistoryErrorBlock(state = state)
    }
}

private fun LazyListScope.loadingItems(state: TxHistoryUM.Loading) {
    item(key = "tx_history_loading", contentType = "tx_history_loading") {
        TxHistoryLoadingBlock(state = state)
    }
}

private fun LazyListScope.notSupportedItem(state: TxHistoryUM.NotSupported) {
    item(key = "tx_history_not_supported", contentType = "tx_history_not_supported") {
        TxHistoryNotSupportedBlock(state = state)
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun TxHistoryContentBlock(state: TxHistoryUM.Content, modifier: Modifier = Modifier) {
    // TODO [REDACTED_TASK_KEY] redesign Content state
}

@Composable
private fun TxHistoryEmptyBlock(state: TxHistoryUM.Empty, modifier: Modifier = Modifier) {
    EmptyTransactionBlock(
        state = EmptyTransactionsBlockState.Empty(
            onExplore = state.onExploreClick,
            exploreIconResId = R.drawable.ic_compass_24,
        ),
        modifier = modifier,
    )
}

@Composable
private fun TxHistoryErrorBlock(state: TxHistoryUM.Error, modifier: Modifier = Modifier) {
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
private fun TxHistoryLoadingBlock(state: TxHistoryUM.Loading, modifier: Modifier = Modifier) {
    val transactionCount = state.items.count { it is TxHistoryUM.TxHistoryItemUM.Transaction }
    Column(modifier = modifier.fillMaxWidth()) {
        var transactionIndex = 0
        state.items.forEach { item ->
            when (item) {
                is TxHistoryUM.TxHistoryItemUM.Title -> TxHistoryLoadingTitle()
                is TxHistoryUM.TxHistoryItemUM.Transaction -> {
                    val fraction = if (transactionCount <= 1) {
                        0f
                    } else {
                        transactionIndex.toFloat() / (transactionCount - 1)
                    }
                    val alpha = lerp(start = 1f, stop = LOADING_TRANSACTION_MIN_ALPHA, fraction = fraction)
                    TxHistoryLoadingTransaction(modifier = Modifier.alpha(alpha))
                    transactionIndex++
                }
                is TxHistoryUM.TxHistoryItemUM.GroupTitle -> Unit
            }
        }
    }
}

@Composable
private fun TxHistoryLoadingTitle(modifier: Modifier = Modifier) {
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
private fun TxHistoryNotSupportedBlock(state: TxHistoryUM.NotSupported, modifier: Modifier = Modifier) {
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
            state = TxHistoryUM.Loading(
                isBalanceHidden = false,
                onExploreClick = {},
            ),
        )
    }
}
// endregion Preview