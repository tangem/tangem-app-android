package com.tangem.features.feed.ui.earn

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SmallButtonShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.ui.earn.components.EarnItemPlaceholder
import com.tangem.features.feed.ui.earn.components.EarnListItem
import com.tangem.features.feed.ui.earn.components.MostlyUsedCard
import com.tangem.features.feed.ui.earn.components.MostlyUsedPlaceholder
import com.tangem.features.feed.ui.earn.state.*
import kotlinx.collections.immutable.persistentListOf

private const val EARN_LOAD_MORE_BUFFER = 3

@Composable
internal fun EarnContent(state: EarnUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
    val listState = rememberLazyListState()

    if (state.bestOpportunities is EarnBestOpportunitiesUM.Content) {
        PaginationHandler(
            listState = listState,
            state = state.bestOpportunities,
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(background),
        contentPadding = PaddingValues(bottom = bottomBarHeight),
    ) {
        item(key = "mostly_used_header") {
            SectionHeader(
                title = stringResourceSafe(R.string.earn_mostly_used),
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        item(key = "mostly_used_content") {
            MostlyUsedContent(
                state = state.mostlyUsed,
                onScroll = state.onSliderScroll,
            )
        }

        item(key = "best_opportunities_header") {
            SectionHeader(
                title = stringResourceSafe(R.string.earn_best_opportunities),
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        item(key = "best_opportunities_filters") {
            SpacerH(12.dp)
            BestOpportunitiesFilters(
                state = state.bestOpportunities,
                earnFilterUM = state.earnFilterUM,
                onNetworkFilterClick = state.onNetworkFilterClick,
                onTypeFilterClick = state.onTypeFilterClick,
            )
        }

        bestOpportunitiesItems(
            state = state.bestOpportunities,
        )
    }
}

@Composable
private fun MostlyUsedContent(state: EarnListUM, onScroll: () -> Unit) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class.java },
    ) { animatedState ->
        when (animatedState) {
            is EarnListUM.Loading -> {
                MostlyUsedPlaceholder()
            }
            is EarnListUM.Content -> {
                LazyRow(
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = animatedState.items,
                        key = { _, item -> "${item.tokenName}-${item.network}" },
                    ) { index, item ->
                        val cardModifier = Modifier.conditional(
                            condition = index == FOURTH_ITEM_INDEX,
                            modifier = {
                                onFirstVisible(
                                    minFractionVisible = 0.5f,
                                    callback = onScroll,
                                )
                            },
                        )
                        MostlyUsedCard(
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
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(
                            color = TangemTheme.colors.background.action,
                            shape = TangemTheme.shapes.roundedCornersXMedium,
                        )
                        .padding(vertical = 32.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    UnableToLoadData(onRetryClick = animatedState.onRetryClicked)
                }
            }
            EarnListUM.Empty -> Unit // no need to handle
        }
    }
}

@Composable
private fun BestOpportunitiesFilters(
    state: EarnBestOpportunitiesUM,
    earnFilterUM: EarnFilterUM,
    onNetworkFilterClick: () -> Unit,
    onTypeFilterClick: () -> Unit,
) {
    when (state) {
        is EarnBestOpportunitiesUM.Loading -> FilterButtonsShimmer()
        else -> FilterButtons(
            earnFilterUM = earnFilterUM,
            onNetworkFilterClick = onNetworkFilterClick,
            onTypeFilterClick = onTypeFilterClick,
        )
    }
}

private fun LazyListScope.bestOpportunitiesItems(state: EarnBestOpportunitiesUM) {
    when (state) {
        is EarnBestOpportunitiesUM.Loading -> {
            val lastIndex = PLACEHOLDER_ITEMS_COUNT - 1
            items(
                count = PLACEHOLDER_ITEMS_COUNT,
                key = { "placeholder_$it" },
            ) { index ->
                EarnItemPlaceholder(
                    modifier = Modifier
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            lastIndex = lastIndex,
                            backgroundColor = TangemTheme.colors.background.action,
                        ),
                )
            }
        }
        is EarnBestOpportunitiesUM.Empty -> {
            item(key = "best_opportunities_empty") {
                SpacerH(12.dp)
                BestOpportunitiesEmpty()
            }
        }
        is EarnBestOpportunitiesUM.EmptyFiltered -> {
            item(key = "best_opportunities_empty_filtered") {
                SpacerH(12.dp)
                BestOpportunitiesEmptyFiltered(onClearFilterClick = state.onClearFilterClick)
            }
        }
        is EarnBestOpportunitiesUM.Content -> {
            if (state.items.isNotEmpty()) {
                val lastIndex = state.items.lastIndex
                itemsIndexed(
                    items = state.items,
                    key = { _, item -> "${item.tokenName}-${item.network}" },
                ) { index, item ->
                    EarnListItem(
                        item = item,
                        modifier = Modifier
                            .roundedShapeItemDecoration(
                                currentIndex = index,
                                lastIndex = lastIndex,
                                backgroundColor = TangemTheme.colors.background.action,
                            ),
                    )
                }
            }
        }
        is EarnBestOpportunitiesUM.Error -> {
            item(key = "best_opportunities_error") {
                SpacerH(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            color = TangemTheme.colors.background.action,
                            shape = TangemTheme.shapes.roundedCornersXMedium,
                        )
                        .padding(vertical = 32.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    UnableToLoadData(onRetryClick = state.onRetryClicked)
                }
            }
        }
    }
}

@Composable
private fun FilterButtons(
    earnFilterUM: EarnFilterUM,
    onNetworkFilterClick: () -> Unit,
    onTypeFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        SecondarySmallButton(
            config = SmallButtonConfig(
                text = when (earnFilterUM.selectedNetworkFilter) {
                    is EarnFilterNetworkUM.AllNetworks -> TextReference.Res(R.string.earn_filter_all_networks)
                    is EarnFilterNetworkUM.MyNetworks -> TextReference.Res(R.string.earn_filter_my_networks)
                    is EarnFilterNetworkUM.Network -> TextReference.Str(earnFilterUM.selectedNetworkFilter.text)
                },
                onClick = onNetworkFilterClick,
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_chevron_24),
                isEnabled = earnFilterUM.isNetworkFilterEnabled,
            ),
        )

        SpacerWMax()

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = earnFilterUM.selectedTypeFilter.text,
                onClick = onTypeFilterClick,
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_chevron_24),
                isEnabled = earnFilterUM.isTypeFilterEnabled,
            ),
        )
    }
}

@Composable
private fun FilterButtonsShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        SmallButtonShimmer(
            modifier = Modifier.width(110.dp),
        )

        SpacerWMax()

        SmallButtonShimmer(
            modifier = Modifier.width(90.dp),
        )
    }
}

@Composable
private fun BestOpportunitiesEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(vertical = 32.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_empty_64),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        SpacerH(24.dp)
        Text(
            modifier = Modifier
                .padding(horizontal = 32.dp),
            text = stringResourceSafe(R.string.earn_empty),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BestOpportunitiesEmptyFiltered(onClearFilterClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(vertical = 32.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.earn_no_results),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
        SpacerH(12.dp)
        SecondarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(R.string.earn_clear_filter),
                onClick = onClearFilterClick,
            ),
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp),
        text = title,
        style = TangemTheme.typography.h3,
        color = TangemTheme.colors.text.primary1,
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

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnContentPreview() {
    TangemThemePreview {
        val background = TangemTheme.colors.background.tertiary
        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember { mutableStateOf(background) },
        ) {
            EarnContent(
                state = previewEarnUM(
                    mostlyUsed = EarnListUM.Content(
                        items = persistentListOf(
                            previewEarnListItemUM(),
                            previewEarnListItemUM(
                                tokenName = "Cosmos",
                                symbol = "ATOM",
                                network = "Cosmos",
                            ),
                        ),
                    ),
                    bestOpportunities = EarnBestOpportunitiesUM.Content(
                        items = persistentListOf(
                            previewEarnListItemUM(
                                tokenName = "Cosmos Hub",
                                symbol = "ATOM",
                                network = "Cosmos network",
                            ),
                            previewEarnListItemUM(
                                tokenName = "Tether",
                                symbol = "USDT",
                                network = "Ethereum Network",
                            ),
                        ),
                        onLoadMore = {},
                    ),
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnContentLoadingPreview() {
    TangemThemePreview {
        val background = TangemTheme.colors.background.tertiary
        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember { mutableStateOf(background) },
        ) {
            EarnContent(
                state = previewEarnUM(
                    mostlyUsed = EarnListUM.Error(onRetryClicked = {}),
                    bestOpportunities = EarnBestOpportunitiesUM.Loading,
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnContentErrorPreview() {
    TangemThemePreview {
        val background = TangemTheme.colors.background.tertiary
        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember { mutableStateOf(background) },
        ) {
            EarnContent(
                state = previewEarnUM(
                    mostlyUsed = EarnListUM.Content(
                        items = persistentListOf(
                            previewEarnListItemUM(),
                            previewEarnListItemUM(
                                tokenName = "Cosmos",
                                symbol = "ATOM",
                                network = "Cosmos",
                            ),
                        ),
                    ),
                    bestOpportunities = EarnBestOpportunitiesUM.Error(onRetryClicked = {}),
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnContentEmptyPreview() {
    TangemThemePreview {
        val background = TangemTheme.colors.background.tertiary
        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember { mutableStateOf(background) },
        ) {
            EarnContent(
                state = previewEarnUM(
                    mostlyUsed = EarnListUM.Content(
                        items = persistentListOf(
                            previewEarnListItemUM(),
                            previewEarnListItemUM(
                                tokenName = "Cosmos",
                                symbol = "ATOM",
                                network = "Cosmos",
                            ),
                        ),
                    ),
                    bestOpportunities = EarnBestOpportunitiesUM.Empty,
                ),
            )
        }
    }
}

@Composable
private fun previewEarnListItemUM(
    tokenName: String = "USDC",
    symbol: String = "USDC",
    network: String = "Ethereum",
): EarnListItemUM = EarnListItemUM(
    network = stringReference(network),
    symbol = stringReference(symbol),
    tokenName = stringReference(tokenName),
    currencyIconState = CurrencyIconState.TokenIcon(
        url = null,
        topBadgeIconResId = R.drawable.img_eth_22,
        fallbackTint = TangemColorPalette.Black,
        fallbackBackground = TangemColorPalette.Meadow,
        isGrayscale = false,
        shouldShowCustomBadge = false,
    ),
    earnValue = stringReference("APY 6.54%"),
    earnType = stringReference("Yield"),
    onItemClick = {},
)

private fun previewEarnUM(
    mostlyUsed: EarnListUM = EarnListUM.Loading,
    bestOpportunities: EarnBestOpportunitiesUM = EarnBestOpportunitiesUM.Loading,
): EarnUM = EarnUM(
    mostlyUsed = mostlyUsed,
    bestOpportunities = bestOpportunities,
    earnFilterUM = EarnFilterUM(
        selectedTypeFilter = EarnFilterTypeUM.All,
        selectedNetworkFilter = EarnFilterNetworkUM.AllNetworks(isSelected = true),
        isTypeFilterEnabled = true,
        isNetworkFilterEnabled = true,
    ),
    onBackClick = {},
    onNetworkFilterClick = {},
    onTypeFilterClick = {},
    onSliderScroll = {},
)

private const val PLACEHOLDER_ITEMS_COUNT = 8
private const val FOURTH_ITEM_INDEX = 3