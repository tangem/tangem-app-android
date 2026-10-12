package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.ui.state.MarketPulseListUM
import com.tangem.features.feed.ui.FeedSearchSectionHeader

private const val LOAD_NEXT_PAGE_ON_END_INDEX = 10
private const val MARKET_SHIMMER_COUNT = 10

@Composable
internal fun CryptoEmptySearchContent(
    state: MarketPulseListUM,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = bottomPadding,
        ),
    ) {
        item(key = "header") {
            header()
        }

        item(key = "marketHeader") {
            FeedSearchSectionHeader(
                title = stringResourceSafe(R.string.markets_common_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        when (state) {
            is MarketPulseListUM.Loading -> marketTokenShimmers(
                count = MARKET_SHIMMER_COUNT,
                horizontalPadding = 4.dp,
            )
            is MarketPulseListUM.Error -> item(key = "marketError") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    UnableToLoadData(onRetryClick = state.onRetry)
                }
            }
            is MarketPulseListUM.Content -> marketTokenItems(
                items = state.items,
                horizontalPadding = 4.dp,
            )
        }
    }

    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_NEXT_PAGE_ON_END_INDEX,
        triggerLoadMoreCheckOnItemsCountChange = true,
        onLoadMore = remember(state) {
            {
                if (state is MarketPulseListUM.Content) {
                    state.loadMore()
                    true
                } else {
                    false
                }
            }
        },
    )
}