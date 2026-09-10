package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.crypto.ui.state.*
import kotlinx.collections.immutable.ImmutableList

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
            promoBanners(Modifier.padding(vertical = 8.dp))
        }
        if (totalMarketCap !is TotalMarketCapUM.Empty) {
            item(key = "totalMarketCap") {
                TotalMarketCapBlock(
                    state = totalMarketCap,
                    modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
                )
            }
        }
        item(key = "marketPulseHeader") {
            MarketPulseHeader(
                title = marketPulse.title,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        item(key = "marketPulseControls") {
            MarketPulseControls(
                categoriesFilter = marketPulse.categoriesFilter,
                intervalMenu = marketPulse.intervalMenu,
            )
        }
        item(key = "marketPulseCategories") {
            MarketPulseCategories(
                categories = marketPulse.categories,
                selectedIndex = marketPulse.selectedCategoryIndex,
                onSelect = marketPulse.onCategorySelect,
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
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
        } else {
            Spacer(modifier = Modifier.weight(weight = 1f))
        }
        SpacerH(8.dp)
        IntervalFilter(intervalMenu = intervalMenu)
    }
}

@Composable
private fun IntervalFilter(intervalMenu: MarketPulseIntervalUM, modifier: Modifier = Modifier) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val hapticManager = LocalHapticManager.current

    Box(modifier = modifier) {
        TangemFilterItem(
            state = TangemFilterItemUM.Inactive(
                id = "interval",
                label = intervalMenu.selected.intervalTitle(),
                onClick = {
                    hapticManager.perform(TangemHapticEffect.View.ContextClick)
                    isMenuExpanded = true
                },
            ),
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

@Composable
private fun MarketPulseCategories(
    categories: ImmutableList<MarketPulseCategoryUM>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        itemsIndexed(items = categories, key = { _, category -> category.id }) { index, category ->
            MarketPulseCategory(
                category = category,
                isSelected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun MarketPulseCategory(
    category: MarketPulseCategoryUM,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(color = if (isSelected) TangemTheme.colors3.bg.tertiary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = category.title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = if (isSelected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
            maxLines = 1,
        )
    }
}