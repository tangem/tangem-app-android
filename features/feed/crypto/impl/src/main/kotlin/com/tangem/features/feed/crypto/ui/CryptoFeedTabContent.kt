package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.ui.state.CryptoFeedTabUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseIntervalUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseListUM
import com.tangem.features.feed.crypto.ui.state.TotalMarketCapUM

private const val LOAD_NEXT_PAGE_ON_END_INDEX = 50
private val ROW_HORIZONTAL_PADDING = 4.dp

/**
 * Crypto feed tab: promo banners, the Total market cap block, and the Market Pulse markets list
 * (DS3 token rows with mini charts, infinite pagination).
 *
 * @param listState hoisted to the tab component so scroll survives tab switches
 * @param contentPadding insets of the feed chrome pinned above the list
 * @param promoBanners promo banners block slot, rendered as the first list item
 */
@Composable
internal fun CryptoFeedTabContent(
    state: CryptoFeedTabUM,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    promoBanners: @Composable (Modifier) -> Unit,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    val marketPulse = state.marketPulse
    val totalMarketCap = state.totalMarketCap

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = bottomPadding,
        ),
    ) {
        item(key = "promoBanners") {
            promoBanners(Modifier.padding(bottom = 20.dp))
        }
        if (totalMarketCap !is TotalMarketCapUM.Empty) {
            item(key = "totalMarketCap") {
                TotalMarketCapBlock(
                    state = totalMarketCap,
                    modifier = Modifier.padding(bottom = 48.dp),
                )
            }
        }
        item(key = "marketPulseHeader") {
            MarketPulseHeader(
                title = marketPulse.title,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        item(key = "marketPulseControls") {
            MarketPulseControls(
                categoriesFilter = marketPulse.categoriesFilter,
                intervalMenu = marketPulse.intervalMenu,
            )
        }
        item(key = "marketPulseSortTabs") {
            TangemTabNavigation(
                tabs = marketPulse.sortTabs,
                variant = TangemTabItem.Variant.Transparent,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
        marketPulseListItems(list = marketPulse.list)
    }

    VisibleTokensTracker(
        listState = listState,
        isTrackingEnabled = marketPulse.list is MarketPulseListUM.Content,
        onVisibleItemsChange = marketPulse.onVisibleItemsChange,
    )

    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_NEXT_PAGE_ON_END_INDEX,
        triggerLoadMoreCheckOnItemsCountChange = true,
        onLoadMore = remember(marketPulse.list) {
            {
                val list = marketPulse.list
                if (list is MarketPulseListUM.Content) {
                    list.loadMore()
                    true
                } else {
                    false
                }
            }
        },
    )
}

private fun LazyListScope.marketPulseListItems(list: MarketPulseListUM) {
    when (list) {
        is MarketPulseListUM.Loading -> marketTokenShimmers(horizontalPadding = ROW_HORIZONTAL_PADDING)
        is MarketPulseListUM.Error -> item(key = "loadingError") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                UnableToLoadData(onRetryClick = list.onRetry)
            }
        }
        is MarketPulseListUM.Content -> marketTokenItems(list.items, horizontalPadding = ROW_HORIZONTAL_PADDING)
    }
}

@Composable
private fun MarketPulseHeader(title: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun MarketPulseControls(
    categoriesFilter: TangemFilterItemUM?,
    intervalMenu: MarketPulseIntervalUM,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (categoriesFilter != null) {
            TangemFilterItem(
                state = categoriesFilter,
                variant = TangemFilterItem.Variant.Transparent,
                modifier = Modifier.weight(weight = 1f, fill = false),
                clearContentDescription = stringResourceSafe(R.string.common_reset),
            )
        } else {
            Spacer(modifier = Modifier.weight(weight = 1f))
        }
        SpacerW(8.dp)
        IntervalFilter(intervalMenu = intervalMenu)
    }
}

@Composable
private fun IntervalFilter(intervalMenu: MarketPulseIntervalUM, modifier: Modifier = Modifier) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val hapticManager = LocalHapticManager.current

    Box(modifier = modifier) {
        val onClick = {
            hapticManager.perform(TangemHapticEffect.View.ContextClick)
            isMenuExpanded = true
        }
        TangemFilterItem(
            variant = TangemFilterItem.Variant.Transparent,
            state = if (intervalMenu.selected == intervalMenu.default) {
                TangemFilterItemUM.Inactive(
                    id = INTERVAL_FILTER_ID,
                    label = intervalMenu.selected.intervalTitle(),
                    onClick = onClick,
                )
            } else {
                TangemFilterItemUM.Active(
                    id = INTERVAL_FILTER_ID,
                    value = intervalMenu.selected.intervalTitle(),
                    onClick = onClick,
                    onClearClick = { intervalMenu.onOptionClick(intervalMenu.default) },
                )
            },
            clearContentDescription = stringResourceSafe(R.string.common_reset),
        )
        MarketPulseIntervalMenu(
            um = intervalMenu,
            expanded = isMenuExpanded,
            onDismiss = { isMenuExpanded = false },
            modifier = Modifier
                .align(Alignment.TopStart)
                .hazeEffectTangem { blurRadius = 10.dp },
        )
    }
}

private const val INTERVAL_FILTER_ID = "interval"