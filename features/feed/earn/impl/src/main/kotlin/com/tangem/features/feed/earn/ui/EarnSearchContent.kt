package com.tangem.features.feed.earn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.markets.tokenselector.tokenGroupRowDecoration
import com.tangem.common.ui.markets.tokenselector.tokenSelectorSectionItems
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.ds2.tokenrow.Shimmer
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.feed.earn.ui.state.EarnSearchListUM
import com.tangem.features.feed.earn.ui.state.EarnSearchPortfolioUM
import com.tangem.features.feed.earn.ui.state.EarnSearchUM
import com.tangem.features.feed.ui.FeedSearchNoResults
import com.tangem.features.feed.ui.FeedSearchSectionHeader

private const val EARN_SHIMMER_COUNT = 8

@Composable
internal fun EarnSearchContent(
    state: EarnSearchUM,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    val hasPortfolio = state.portfolio is EarnSearchPortfolioUM.Content

    // a new query is a different result set, so start reading it from the top
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
        if (state.portfolio is EarnSearchPortfolioUM.Content) {
            item(key = "portfolioHeader") {
                FeedSearchSectionHeader(title = stringResourceSafe(R.string.markets_common_my_portfolio))
            }
            // the list is already inset to the card width, so the wallet name needs no inset of its own
            tokenSelectorSectionItems(state.portfolio.sections, walletHeaderHorizontalPadding = 0.dp)
        }

        earnSectionItems(earn = state.earn, hasPortfolioSection = hasPortfolio)
    }
}

private fun LazyListScope.earnSectionItems(earn: EarnSearchListUM, hasPortfolioSection: Boolean) {
    if (earn is EarnSearchListUM.NotFound) {
        item(key = "earnNotFound") {
            FeedSearchNoResults(shouldFillParent = !hasPortfolioSection)
        }
        return
    }

    if (hasPortfolioSection) {
        item(key = "sectionSpacer") { SpacerH(24.dp) }
    }
    item(key = "earnHeader") {
        FeedSearchSectionHeader(title = stringResourceSafe(R.string.earn_assets))
    }

    when (earn) {
        is EarnSearchListUM.Loading -> items(
            count = EARN_SHIMMER_COUNT,
            key = { "earnShimmer_$it" },
        ) { index ->
            TangemTokenRow.Shimmer(
                modifier = Modifier.tokenGroupRowDecoration(
                    currentIndex = index,
                    lastIndex = EARN_SHIMMER_COUNT - 1,
                ),
            )
        }
        is EarnSearchListUM.Error -> item(key = "earnError") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                UnableToLoadData(onRetryClick = earn.onRetryClicked)
            }
        }
        is EarnSearchListUM.Content -> itemsIndexed(
            items = earn.items,
            key = { _, item -> item.id },
        ) { index, item ->
            TangemTokenRow(
                state = item,
                modifier = Modifier.tokenGroupRowDecoration(
                    currentIndex = index,
                    lastIndex = earn.items.lastIndex,
                ),
            )
        }
        else -> Unit
    }
}