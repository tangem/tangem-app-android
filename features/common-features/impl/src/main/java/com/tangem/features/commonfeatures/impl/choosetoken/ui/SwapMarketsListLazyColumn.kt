package com.tangem.features.commonfeatures.impl.choosetoken.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.tangem.common.ui.markets.MarketsListItem
import com.tangem.common.ui.markets.MarketsListItemPlaceholder
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.tabs.TangemTab
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketCategoriesUM
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState

internal fun LazyListScope.swapMarketsListItems(state: SwapMarketState) {
    item {
        val totalCount = (state as? SwapMarketState.Content)?.total
        Text(
            text = buildAnnotatedString {
                append(state.marketsTitle.resolveReference())
                if (totalCount != null && state.shouldAssetsCount) {
                    withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
                        append(" $totalCount")
                    }
                }
            },
            style = TangemTheme.typography2.headingSemibold20,
            color = TangemTheme.colors2.text.neutral.primary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = TangemTheme.dimens.spacing16),
        )
    }
    state.categories?.let { categories ->
        item(key = "market_categories") {
            MarketCategoriesRow(categories = categories)
        }
    }
    when (state) {
        is SwapMarketState.Loading -> {
            items(count = 100, key = { "market_placeholder_$it" }) {
                MarketsListItemPlaceholder()
            }
        }
        is SwapMarketState.LoadingError -> {
            item(key = "market_loading_error") {
                LoadingErrorItem(
                    modifier = Modifier.fillParentMaxWidth(),
                    onTryAgain = state.onRetryClicked,
                )
            }
        }
        SwapMarketState.SearchNothingFound -> {
            item(key = "market_not_found") {
                SearchNothingFoundText(
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
        }
        is SwapMarketState.Content -> {
            itemsIndexed(
                items = state.items,
                key = { _, item -> item.getComposeKey() },
            ) { index, item ->
                MarketsListItem(
                    model = item,
                    onClick = { state.onItemClick(item) },
                    modifier = Modifier.roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = state.items.lastIndex,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MarketCategoriesRow(categories: SwapMarketCategoriesUM, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing8,
            ),
        contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        items(
            items = categories.items,
            key = { category -> category.name },
        ) { category ->
            TangemTab(
                text = category.title,
                isChecked = category == categories.selected,
                onCheckedChange = { categories.onCategoryClick(category) },
            )
        }
    }
}

@Composable
private fun LoadingErrorItem(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            ),
        contentAlignment = Alignment.Center,
    ) {
        UnableToLoadData(onRetryClick = onTryAgain)
    }
}

@Composable
private fun SearchNothingFoundText(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(TangemTheme.dimens.spacing16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResourceSafe(R.string.markets_search_token_no_result_title),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}