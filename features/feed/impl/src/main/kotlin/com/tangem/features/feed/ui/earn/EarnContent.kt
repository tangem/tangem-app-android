package com.tangem.features.feed.ui.earn

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SmallButtonShimmer
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.feed.ui.earn.components.EarnFilterByNetworkBottomSheet
import com.tangem.features.feed.ui.earn.components.EarnFilterByTypeBottomSheet
import com.tangem.features.feed.ui.earn.components.EarnItemPlaceholder
import com.tangem.features.feed.ui.earn.components.EarnListItem
import com.tangem.features.feed.ui.earn.components.MostlyUsedPlaceholder
import com.tangem.features.feed.ui.earn.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.features.feed.ui.earn.state.EarnListItemUM
import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.earn.state.EarnUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun EarnContent(state: EarnUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }

    EarnFilterByTypeBottomSheet(config = state.filterByTypeBottomSheet)
    EarnFilterByNetworkBottomSheet(config = state.filterByNetworkBottomSheet)

    LazyColumn(
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
            MostlyUsedContent(state = state.mostlyUsed)
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
                selectedNetworkFilterText = state.selectedNetworkFilterText,
                selectedTypeFilterText = state.selectedTypeFilterText,
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
private fun MostlyUsedContent(state: EarnListUM) {
    AnimatedContent(state) { st ->
        when (st) {
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
                    items(
                        items = st.items,
                        key = { "${it.tokenName}-${it.network}" },
                    ) { item ->
                        MostlyUsedCard(
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
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    UnableToLoadData(onRetryClick = st.onRetryClicked)
                }
            }
        }
    }
}

@Composable
private fun MostlyUsedCard(item: EarnListItemUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(148.dp)
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        CurrencyIcon(
            modifier = Modifier.size(32.dp),
            state = item.currencyIconState,
            shouldDisplayNetwork = true,
            networkBadgeSize = 12.dp,
            networkBadgeBackground = TangemTheme.colors.background.action,
        )

        SpacerH(8.dp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.tokenName.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle2,
                maxLines = 1,
            )
            SpacerW(4.dp)
            Text(
                text = item.symbol.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
                maxLines = 1,
            )
        }

        SpacerH(2.dp)

        Text(
            text = item.earnValue.resolveReference(),
            color = TangemTheme.colors.text.accent,
            style = TangemTheme.typography.caption1,
            maxLines = 1,
        )
    }
}

@Composable
private fun BestOpportunitiesFilters(
    state: EarnBestOpportunitiesUM,
    selectedNetworkFilterText: TextReference,
    selectedTypeFilterText: TextReference,
    onNetworkFilterClick: () -> Unit,
    onTypeFilterClick: () -> Unit,
) {
    AnimatedContent(state) { st ->
        when (st) {
            is EarnBestOpportunitiesUM.Loading -> {
                FilterButtonsShimmer()
            }
            is EarnBestOpportunitiesUM.EmptyFiltered,
            is EarnBestOpportunitiesUM.Content,
            -> {
                FilterButtons(
                    selectedNetworkFilterText = selectedNetworkFilterText,
                    selectedTypeFilterText = selectedTypeFilterText,
                    isEnabled = true,
                    onNetworkFilterClick = onNetworkFilterClick,
                    onTypeFilterClick = onTypeFilterClick,
                )
            }
            is EarnBestOpportunitiesUM.Empty,
            is EarnBestOpportunitiesUM.Error,
            -> {
                FilterButtons(
                    selectedNetworkFilterText = selectedNetworkFilterText,
                    selectedTypeFilterText = selectedTypeFilterText,
                    isEnabled = false,
                    onNetworkFilterClick = onNetworkFilterClick,
                    onTypeFilterClick = onTypeFilterClick,
                )
            }
        }
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
    selectedNetworkFilterText: TextReference,
    selectedTypeFilterText: TextReference,
    isEnabled: Boolean,
    onNetworkFilterClick: () -> Unit,
    onTypeFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        SecondarySmallButton(
            config = SmallButtonConfig(
                text = selectedNetworkFilterText,
                onClick = onNetworkFilterClick,
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_chevron_24),
                isEnabled = isEnabled,
            ),
        )

        SpacerWMax()

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = selectedTypeFilterText,
                onClick = onTypeFilterClick,
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_chevron_24),
                isEnabled = isEnabled,
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
            .padding(horizontal = 16.dp),
        text = title,
        style = TangemTheme.typography.h3,
        color = TangemTheme.colors.text.primary1,
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
                    mostlyUsed = EarnListUM.Loading,
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
    selectedTypeFilter = EarnFilterTypeUM.All,
    selectedNetworkFilter = EarnFilterNetworkUM.AllNetworks(
        text = resourceReference(R.string.earn_filter_all_networks),
        isSelected = true,
    ),
    filterByTypeBottomSheet = TangemBottomSheetConfig.Empty,
    filterByNetworkBottomSheet = TangemBottomSheetConfig.Empty,
    onBackClick = {},
    onNetworkFilterClick = {},
    onTypeFilterClick = {},
)

private const val PLACEHOLDER_ITEMS_COUNT = 8