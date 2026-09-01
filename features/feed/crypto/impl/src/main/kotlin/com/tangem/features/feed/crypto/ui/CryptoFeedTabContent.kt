package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.crypto.ui.state.CryptoFeedTabUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseCategoryUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseListUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseUM
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
        item(key = "totalMarketCap") {
            TotalMarketCapBlock(
                state = state.totalMarketCap,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        item(key = "marketPulseHeader") {
            MarketPulseHeader(
                state = marketPulse,
                modifier = Modifier.padding(top = 16.dp),
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
private fun MarketPulseHeader(state: MarketPulseUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        TangemFilterItem(state = state.interval)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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