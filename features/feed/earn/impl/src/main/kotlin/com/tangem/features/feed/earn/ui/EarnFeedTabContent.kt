package com.tangem.features.feed.earn.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds2.filter.TangemFilterGroup
import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.Shimmer
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.styledStringReference
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.earn.ui.components.BestOpportunitiesEmpty
import com.tangem.features.feed.earn.ui.components.BestOpportunitiesEmptyFiltered
import com.tangem.features.feed.earn.ui.components.OpportunitiesCard
import com.tangem.features.feed.earn.ui.components.OpportunitiesCardPlaceholder
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.features.feed.earn.ui.state.EarnListUM
import com.tangem.features.feed.earn.ui.state.EarnOpportunitiesItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val EARN_LOAD_MORE_BUFFER = 3

/**
 * Earn feed tab: the Mostly used carousel and the Best opportunities list with filters.
 *
 * @param listState hoisted to the tab component so scroll survives tab switches
 * @param contentPadding insets of the feed chrome pinned above the list
 * @param promoBanners promo banners block slot, rendered as the first list item
 */
@Composable
internal fun EarnFeedTabContent(
    state: EarnFeedTabUM,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    promoBanners: @Composable (Modifier) -> Unit,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    if (state.bestOpportunities is EarnBestOpportunitiesUM.Content) {
        PaginationHandler(
            listState = listState,
            state = state.bestOpportunities,
        )
    }

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

        item(key = "opportunities") {
            OpportunitiesContent(
                state = state.mostlyUsed,
                onScroll = state.onSliderScroll,
            )
        }

        item(key = "best_opportunities_header") {
            SectionHeader(
                title = stringResourceSafe(R.string.earn_assets),
                modifier = Modifier.padding(
                    top = if (state.mostlyUsed == EarnListUM.Empty) {
                        24.dp
                    } else {
                        48.dp
                    },
                ),
            )
        }

        item(key = "best_opportunities_filters") {
            SpacerH(12.dp)
            TangemFilterGroup(
                items = state.filters,
                arrangement = Arrangement.SpaceBetween,
                variant = TangemFilterItem.Variant.Transparent,
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(16.dp)
        }

        bestOpportunitiesItems(state = state.bestOpportunities)
    }
}

@Composable
private fun OpportunitiesContent(state: EarnListUM, onScroll: () -> Unit) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class.java },
    ) { animatedState ->
        Column {
            if (animatedState != EarnListUM.Empty) {
                SectionHeader(
                    title = stringResourceSafe(R.string.common_featured),
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                )
            }
            when (animatedState) {
                is EarnListUM.Loading -> {
                    OpportunitiesCardPlaceholder()
                }
                is EarnListUM.Content -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = animatedState.items,
                            key = { _, item -> item.id },
                        ) { index, item ->
                            val cardModifier = Modifier.conditional(
                                condition = index == FOURTH_ITEM_INDEX,
                                modifier = { onFirstVisible(minFractionVisible = 0.5f, callback = onScroll) },
                            )
                            OpportunitiesCard(
                                modifier = cardModifier,
                                item = item,
                                onClick = item.onItemClick,
                            )
                        }
                    }
                }
                is EarnListUM.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(130.dp)
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { UnableToLoadData(onRetryClick = animatedState.onRetryClicked) }
                }
                EarnListUM.Empty -> Unit // no need to handle
            }
        }
    }
}

private fun LazyListScope.bestOpportunitiesItems(state: EarnBestOpportunitiesUM) {
    when (state) {
        is EarnBestOpportunitiesUM.Loading -> {
            items(
                count = PLACEHOLDER_ITEMS_COUNT,
                key = { "placeholder_$it" },
            ) {
                TangemTokenRow.Shimmer()
            }
        }
        is EarnBestOpportunitiesUM.Empty -> {
            item(key = "best_opportunities_empty") {
                BestOpportunitiesEmpty()
            }
        }
        is EarnBestOpportunitiesUM.EmptyFiltered -> {
            item(key = "best_opportunities_empty_filtered") {
                BestOpportunitiesEmptyFiltered(onClearFilterClick = state.onClearFilterClick)
            }
        }
        is EarnBestOpportunitiesUM.Content -> {
            if (state.items.isNotEmpty()) {
                items(
                    items = state.items,
                    key = { item -> item.id },
                ) { item ->
                    TangemTokenRow(state = item)
                }
            }
        }
        is EarnBestOpportunitiesUM.Error -> {
            item(key = "best_opportunities_error") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 142.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    UnableToLoadData(onRetryClick = state.onRetryClicked)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        text = title,
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
    )
}

@Composable
private fun PaginationHandler(listState: LazyListState, state: EarnBestOpportunitiesUM.Content) {
    InfiniteListHandler(
        listState = listState,
        buffer = EARN_LOAD_MORE_BUFFER,
        triggerLoadMoreCheckOnItemsCountChange = true,
        onLoadMore = remember(state) {
            {
                state.onLoadMore()
                true
            }
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun EarnContent_Preview(@PreviewParameter(EarnContentPreviewProvider::class) params: EarnFeedTabUM) {
    TangemThemePreviewRedesign {
        val background = LocalMainBottomSheetColor.current.value
        Surface(contentColor = background) {
            EarnFeedTabContent(
                state = params,
                listState = rememberLazyListState(),
                contentPadding = PaddingValues(),
                promoBanners = {},
            )
        }
    }
}

private class EarnContentPreviewProvider : PreviewParameterProvider<EarnFeedTabUM> {
    override val values: Sequence<EarnFeedTabUM>
        get() = sequenceOf(
            previewEarnFeedTabUM(
                mostlyUsed = EarnListUM.Content(
                    items = persistentListOf(
                        previewMostlyUsedItemUM(),
                        previewMostlyUsedItemUM(tokenName = "Cosmos", symbol = "ATOM"),
                    ),
                ),
                bestOpportunities = EarnBestOpportunitiesUM.Content(
                    items = persistentListOf(
                        previewTokenRowState(tokenName = "Cosmos Hub", network = "Cosmos network"),
                        previewTokenRowState(tokenName = "Tether", network = "Ethereum Network"),
                    ),
                    onLoadMore = {},
                ),
            ),
            previewEarnFeedTabUM(
                mostlyUsed = EarnListUM.Error(onRetryClicked = {}),
                bestOpportunities = EarnBestOpportunitiesUM.Loading,
                filters = previewLoadingFilters(),
            ),
            previewEarnFeedTabUM(
                mostlyUsed = EarnListUM.Content(
                    items = persistentListOf(
                        previewMostlyUsedItemUM(),
                        previewMostlyUsedItemUM(tokenName = "Cosmos", symbol = "ATOM"),
                    ),
                ),
                bestOpportunities = EarnBestOpportunitiesUM.Error(onRetryClicked = {}),
            ),
            previewEarnFeedTabUM(
                mostlyUsed = EarnListUM.Content(
                    items = persistentListOf(
                        previewMostlyUsedItemUM(),
                        previewMostlyUsedItemUM(tokenName = "Cosmos", symbol = "ATOM"),
                    ),
                ),
                bestOpportunities = EarnBestOpportunitiesUM.Empty,
            ),
            previewEarnFeedTabUM(
                mostlyUsed = EarnListUM.Empty,
                bestOpportunities = EarnBestOpportunitiesUM.Content(
                    items = persistentListOf(
                        previewTokenRowState(tokenName = "Cosmos Hub", network = "Cosmos network"),
                        previewTokenRowState(tokenName = "Tether", network = "Ethereum Network"),
                    ),
                    onLoadMore = {},
                ),
            ),
        )
}
// endregion

private fun previewMostlyUsedItemUM(tokenName: String = "USDC", symbol: String = "USDC"): EarnOpportunitiesItemUM =
    EarnOpportunitiesItemUM(
        id = tokenName,
        currencyIconState = CurrencyIconState.TokenIcon(
            url = null,
            topBadgeIconResId = R.drawable.img_eth_22,
            fallbackTint = TangemColorPalette.Black,
            fallbackBackground = TangemColorPalette.Meadow,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
        tokenName = stringReference(tokenName),
        symbol = stringReference(symbol),
        earnValue = styledStringReference(
            value = "APY 6.54%",
            spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.status.success) },
        ),
        earnType = stringReference("Staking"),
        onItemClick = {},
    )

private fun previewTokenRowState(
    tokenName: String = "USDC",
    network: String = "Ethereum",
): TangemTokenRow.State.Content = TangemTokenRow.State.Content(
    id = tokenName,
    icon = TangemTokenIcon.UiState.Token(tokenState = TangemTokenIcon.State(url = null)),
    title = stringReference(tokenName),
    quote = stringReference(network),
    fiatBalance = styledStringReference(
        value = "APY 6.54%",
        spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.status.success) },
    ),
    cryptoBalance = stringReference("Yield"),
    onClick = {},
)

private fun previewEarnFeedTabUM(
    mostlyUsed: EarnListUM = EarnListUM.Loading,
    bestOpportunities: EarnBestOpportunitiesUM = EarnBestOpportunitiesUM.Loading,
    filters: ImmutableList<TangemFilterItemUM> = previewFilters(),
): EarnFeedTabUM = EarnFeedTabUM(
    mostlyUsed = mostlyUsed,
    bestOpportunities = bestOpportunities,
    filters = filters,
    onSliderScroll = {},
)

private fun previewFilters(): ImmutableList<TangemFilterItemUM> = persistentListOf(
    TangemFilterItemUM.Inactive(id = "network", label = stringReference("All networks"), onClick = {}),
    TangemFilterItemUM.Inactive(id = "type", label = stringReference("All types"), onClick = {}),
)

private fun previewLoadingFilters(): ImmutableList<TangemFilterItemUM> = persistentListOf(
    TangemFilterItemUM.Loading(id = "network"),
    TangemFilterItemUM.Loading(id = "type"),
)

private const val PLACEHOLDER_ITEMS_COUNT = 8
private const val FOURTH_ITEM_INDEX = 3