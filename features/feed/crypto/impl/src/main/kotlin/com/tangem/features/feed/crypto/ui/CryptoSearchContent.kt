package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.markets.tokenselector.tokenSelectorSectionItems
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.ui.state.CryptoSearchUM
import com.tangem.features.feed.crypto.ui.state.MarketSearchUM
import com.tangem.features.feed.crypto.ui.state.PortfolioSearchUM

private const val LOAD_NEXT_PAGE_ON_END_INDEX = 10
private const val MARKET_SHIMMER_COUNT = 10

/**
 * Crypto tab in search mode: the user's matching holdings, then matching market tokens.
 *
 * Holdings are rendered as wallet/account sections inline — there is no collapsed row and no nested
 * bottom sheet, so every matching holding is visible without a further tap.
 *
 * @param listState hoisted to the tab component so scroll survives tab switches
 */
@Composable
internal fun CryptoSearchContent(
    state: CryptoSearchUM,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    val hasPortfolio = state.portfolio is PortfolioSearchUM.Content

    // legacy behaviour: a new query is a different result set, so start reading it from the top. Keyed
    // on the query and not on the market state, which deliberately stays Content while the query is
    // refined and so would never re-trigger this
    LaunchedEffect(state.query) {
        listState.scrollToItem(0)
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
        if (state.portfolio is PortfolioSearchUM.Content) {
            item(key = "portfolioHeader") {
                SearchSectionHeader(title = stringResourceSafe(R.string.markets_common_my_portfolio))
            }
            // the list is already inset to the card width, so the wallet name needs no inset of its own
            tokenSelectorSectionItems(state.portfolio.sections, walletHeaderHorizontalPadding = 0.dp)
        }

        marketSectionItems(market = state.market, hasPortfolioSection = hasPortfolio)
    }

    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_NEXT_PAGE_ON_END_INDEX,
        triggerLoadMoreCheckOnItemsCountChange = true,
        onLoadMore = remember(state.market) {
            {
                val market = state.market
                if (market is MarketSearchUM.Content) {
                    market.loadMore()
                    true
                } else {
                    false
                }
            }
        },
    )
}

private fun LazyListScope.marketSectionItems(market: MarketSearchUM, hasPortfolioSection: Boolean) {
    if (market is MarketSearchUM.NotFound) {
        item(key = "marketNotFound") {
            NoResults(shouldFillParent = !hasPortfolioSection)
        }
        return
    }

    if (hasPortfolioSection) {
        item(key = "sectionSpacer") { SpacerH(24.dp) }
    }
    item(key = "marketHeader") {
        SearchSectionHeader(title = stringResourceSafe(R.string.markets_common_title))
    }

    when (market) {
        is MarketSearchUM.Loading -> marketTokenShimmers(
            count = MARKET_SHIMMER_COUNT,
            hasGroupedBackground = true,
        )
        is MarketSearchUM.Error -> item(key = "marketError") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                UnableToLoadData(onRetryClick = market.onRetry)
            }
        }
        is MarketSearchUM.Content -> {
            marketTokenItems(market.items, hasGroupedBackground = true)
            if (market.shouldShowUnderMarketCapLimitNotification) {
                item(key = "showTokensUnderMarketCapLimit") {
                    ShowTokensUnderMarketCapLimit(onClick = market.onShowUnderMarketCapLimitClick)
                }
            }
        }
        is MarketSearchUM.NotFound -> Unit // TODO [REDACTED_TASK_KEY], [REDACTED_TASK_KEY]
    }
}

@Composable
private fun SearchSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}

@Composable
private fun ShowTokensUnderMarketCapLimit(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 12.dp, start = 24.dp, end = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.markets_search_see_tokens_under_100k),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            onClick = onClick,
            text = resourceReference(R.string.markets_search_show_tokens),
            size = TangemButton.Size.X8,
        )
    }
}

@Composable
private fun NoResults(shouldFillParent: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = stringResourceSafe(R.string.common_no_results),
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (shouldFillParent) 64.dp else 24.dp),
    )
}