package com.tangem.feature.swap.ui.market

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
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
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R

internal fun LazyListScope.swapMarketsListItems(state: SwapMarketState) {
    item {
        val totalCount = (state as? SwapMarketState.Content)?.total
        Text(
            text = buildAnnotatedString {
                append(stringResourceSafe(R.string.markets_common_title))
                if (totalCount != null) {
                    withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
                        append(" $totalCount")
                    }
                }
            },
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.fillMaxWidth().padding(horizontal = TangemTheme.dimens.spacing16),
        )
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
                        backgroundColor = TangemTheme.colors.background.action,
                    ),
                )
            }
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