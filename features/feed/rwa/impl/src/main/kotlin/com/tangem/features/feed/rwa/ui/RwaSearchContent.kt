package com.tangem.features.feed.rwa.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.markets.tokenselector.tokenSelectorSectionItems
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.feed.rwa.ui.state.RwaSearchListUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchPortfolioUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchUM
import com.tangem.features.feed.ui.FeedSearchNoResults
import com.tangem.features.feed.ui.FeedSearchSectionHeader

private const val LOAD_NEXT_PAGE_ON_END_INDEX = 10
private const val LIST_SHIMMER_COUNT = 10

/**
 * Real assets tab in search mode: the user's matching holdings, then matching real-world assets.
 *
 * Holdings are rendered as wallet/account sections inline — there is no collapsed row and no nested
 * bottom sheet, so every matching holding is visible without a further tap.
 *
 * @param listState hoisted to the tab component so scroll survives tab switches
 */
@Composable
internal fun RwaSearchContent(
    state: RwaSearchUM,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    val hasPortfolio = state.portfolio is RwaSearchPortfolioUM.Content

    // a new query is a different result set, so start reading it from the top. Keyed on the query and not
    // on the list state, which deliberately stays Content while the query is refined and so would never
    // re-trigger this
    LaunchedEffect(state.query, hasPortfolio) {
        listState.requestScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = bottomPadding,
        ),
    ) {
        if (state.portfolio is RwaSearchPortfolioUM.Content) {
            item(key = "portfolioHeader") {
                FeedSearchSectionHeader(title = stringResourceSafe(R.string.markets_common_my_portfolio))
            }
            // the list is already inset to the card width, so the wallet name needs no inset of its own
            tokenSelectorSectionItems(state.portfolio.sections, walletHeaderHorizontalPadding = 0.dp)
        }

        listSectionItems(list = state.list, hasPortfolioSection = hasPortfolio)
    }

    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_NEXT_PAGE_ON_END_INDEX,
        triggerLoadMoreCheckOnItemsCountChange = true,
        onLoadMore = remember(state.list) {
            {
                val list = state.list
                if (list is RwaSearchListUM.Content) {
                    list.loadMore()
                    true
                } else {
                    false
                }
            }
        },
    )
}

private fun LazyListScope.listSectionItems(list: RwaSearchListUM, hasPortfolioSection: Boolean) {
    if (list is RwaSearchListUM.NotFound) {
        item(key = "listNotFound") {
            FeedSearchNoResults(shouldFillParent = !hasPortfolioSection)
        }
        return
    }

    if (hasPortfolioSection) {
        item(key = "sectionSpacer") { SpacerH(24.dp) }
    }
    item(key = "listHeader") {
        FeedSearchSectionHeader(title = stringResourceSafe(R.string.markets_real_assets_title))
    }

    when (list) {
        is RwaSearchListUM.Loading -> rwaTokenShimmers(
            count = LIST_SHIMMER_COUNT,
            hasGroupedBackground = true,
        )
        is RwaSearchListUM.Error -> item(key = "listError") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                UnableToLoadData(onRetryClick = list.onRetry)
            }
        }
        is RwaSearchListUM.Content -> rwaTokenItems(list.items, hasGroupedBackground = true)
        else -> Unit // handled above
    }
}