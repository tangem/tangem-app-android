package com.tangem.features.feed.rwa.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.model.list.RwaInterval
import com.tangem.features.feed.rwa.ui.state.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun RwaFeedTabContent(
    state: RwaFeedTabUM,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    promoBanners: @Composable (Modifier) -> Unit,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

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
        if (state.featured !is RwaFeaturedUM.Empty) {
            item(key = "featuredHeader") {
                SectionTitle(
                    title = resourceReference(R.string.markets_real_assets_featured_assets),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            item(key = "featuredAssets") {
                FeaturedAssets(state = state.featured, modifier = Modifier.padding(bottom = 48.dp))
            }
        }
        item(key = "listHeader") {
            SectionTitle(
                title = resourceReference(R.string.markets_real_assets_title),
            )
        }
        item(key = "listFilters") {
            ListFilters(state = state)
        }
        item(key = "listSortTabs") {
            TangemTabNavigation(
                tabs = state.sortTabs,
                variant = TangemTabItem.Variant.Transparent,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        regionRestrictionItem(banner = state.regionRestriction)
        listItems(list = state.list)
    }

    VisibleTokensTracker(
        listState = listState,
        isTrackingEnabled = state.list is RwaListUM.Content,
        onVisibleItemsChange = state.onVisibleItemsChange,
    )

    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_NEXT_PAGE_ON_END_INDEX,
        triggerLoadMoreCheckOnItemsCountChange = true,
        onLoadMore = remember(state.list) {
            {
                val list = state.list
                if (list is RwaListUM.Content) {
                    list.loadMore()
                    true
                } else {
                    false
                }
            }
        },
    )
}

private fun LazyListScope.regionRestrictionItem(banner: TangemMessageBanner.State?) {
    banner ?: return

    item(key = "regionRestriction") {
        TangemMessageBanner(
            state = banner,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

private fun LazyListScope.listItems(list: RwaListUM) {
    when (list) {
        is RwaListUM.Loading -> rwaTokenShimmers()
        is RwaListUM.Error -> item(key = "listError") {
            ListError(onRetry = list.onRetry)
        }
        is RwaListUM.Content -> rwaTokenItems(list.items)
    }
}

@Composable
private fun SectionTitle(title: TextReference, modifier: Modifier = Modifier) {
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
private fun FeaturedAssets(state: RwaFeaturedUM, modifier: Modifier = Modifier) {
    when (state) {
        is RwaFeaturedUM.Empty -> Unit
        is RwaFeaturedUM.Loading -> LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count = SKELETON_CARD_COUNT) {
                FeaturedAssetCardPlaceholder()
            }
        }
        is RwaFeaturedUM.Content -> FeaturedAssetsRail(assets = state.assets, modifier = modifier)
    }
}

@Composable
private fun FeaturedAssetsRail(assets: ImmutableList<RwaFeaturedAssetUM>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = assets, key = { it.id }) { asset ->
            FeaturedAssetCard(asset = asset)
        }
    }
}

@Composable
private fun FeaturedAssetCard(asset: RwaFeaturedAssetUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(178.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color = TangemTheme.colors3.bg.opaque.primary)
            .clickable(onClick = asset.onClick)
            .padding(all = 12.dp),
    ) {
        TangemTokenIcon(
            state = TangemTokenIcon.UiState.Token(
                TangemTokenIcon.State(url = asset.iconUrl, indicator = asset.indicator),
            ),
            size = TangemTokenIcon.Size.X40,
        )
        SpacerH(22.dp)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f, fill = false),
                text = asset.name.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Text(
                text = asset.ticker.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                lineHeight = (TangemTheme.typography3.caption.medium.lineHeight.value + 1).sp,
                maxLines = 1,
            )
        }
        SpacerH(2.dp)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = asset.price.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
            )
            TangemPriceChange(
                value = asset.priceChange.value,
                direction = asset.priceChange.direction,
            )
        }
    }
}

@Composable
private fun FeaturedAssetCardPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(178.dp)
            .height(130.dp)
            .background(color = TangemTheme.colors3.bg.opaque.primary, shape = RoundedCornerShape(24.dp))
            .padding(all = 12.dp),
    ) {
        TangemShimmer(modifier = Modifier.size(40.dp), radius = 999.dp)
        SpacerH(22.dp)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ShimmerLine(
                style = TangemTheme.typography3.body.medium,
                width = 56.dp,
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
            ShimmerLine(style = TangemTheme.typography3.caption.medium, width = 32.dp)
        }
        SpacerH(2.dp)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ShimmerLine(
                style = TangemTheme.typography3.caption.medium,
                width = 64.dp,
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
            ShimmerLine(style = TangemTheme.typography3.caption.medium, width = 32.dp)
        }
    }
}

@Composable
private fun ShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }

    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun ListFilters(state: RwaFeedTabUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemFilterItem(
            state = state.categoriesFilter,
            variant = TangemFilterItem.Variant.Transparent,
            modifier = Modifier.weight(weight = 1f, fill = false),
            clearContentDescription = stringResourceSafe(R.string.common_reset),
        )
        SpacerW(8.dp)
        IntervalFilter(interval = state.interval)
    }
}

@Composable
private fun IntervalFilter(interval: RwaIntervalUM, modifier: Modifier = Modifier) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val hapticManager = LocalHapticManager.current

    Box(modifier = modifier) {
        val onClick = {
            hapticManager.perform(TangemHapticEffect.View.ContextClick)
            isMenuExpanded = true
        }
        TangemFilterItem(
            variant = TangemFilterItem.Variant.Transparent,
            state = if (interval.selected == interval.default) {
                TangemFilterItemUM.Inactive(id = INTERVAL_FILTER_ID, label = interval.selected.title, onClick = onClick)
            } else {
                TangemFilterItemUM.Active(
                    id = INTERVAL_FILTER_ID,
                    value = interval.selected.title,
                    onClick = onClick,
                    onClearClick = { interval.onOptionClick(interval.default) },
                )
            },
            clearContentDescription = stringResourceSafe(R.string.common_reset),
        )
        RwaIntervalMenu(
            um = interval,
            expanded = isMenuExpanded,
            onDismiss = { isMenuExpanded = false },
            modifier = Modifier
                .align(Alignment.TopStart)
                .hazeEffectTangem { blurRadius = 10.dp },
        )
    }
}

@Composable
private fun ListError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = resourceReference(R.string.common_something_went_wrong).resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_retry),
            onClick = onRetry,
        )
    }
}

private const val LOAD_NEXT_PAGE_ON_END_INDEX = 50
private const val SKELETON_CARD_COUNT = 3

private const val INTERVAL_FILTER_ID = "interval"

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RwaFeedTabContent_Preview(
    @PreviewParameter(RwaFeedTabContentPreviewProvider::class) params: RwaFeedTabUM,
) {
    TangemThemePreviewRedesign {
        RwaFeedTabContent(
            state = params,
            listState = LazyListState(),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.background(color = TangemTheme.colors3.bg.primary),
            promoBanners = {},
        )
    }
}

private class RwaFeedTabContentPreviewProvider : PreviewParameterProvider<RwaFeedTabUM> {
    override val values: Sequence<RwaFeedTabUM>
        get() = sequenceOf(
            previewRwaFeedTabUM(),
        )
}

@Suppress("MagicNumber")
private fun previewRwaFeedTabUM(): RwaFeedTabUM {
    val sparkline = MarketChartRawData(
        x = List(size = 8) { it.toDouble() }.toImmutableList(),
        y = listOf(0.2, 0.5, 0.3, 0.8, 0.6, 1.0, 0.9, 1.2).toImmutableList(),
    )

    return RwaFeedTabUM(
        featured = RwaFeaturedUM.Content(
            assets = persistentListOf(
                previewFeaturedAsset(id = "apple", name = "Apple", ticker = "AAPL", price = "$315.5"),
                previewFeaturedAsset(id = "nvidia", name = "Nvidia", ticker = "NVDA", price = "$210.70"),
            ),
        ),
        categoriesFilter = TangemFilterItemUM.Active(
            id = "rwa_categories",
            value = stringReference("Stocks"),
            onClick = {},
            onClearClick = {},
        ),
        interval = RwaIntervalUM(selected = RwaInterval.H24, default = RwaInterval.H24, onOptionClick = {}),
        sortTabs = persistentListOf(
            TangemTabItemUM.Content(
                id = "market_cap",
                label = stringReference("Market cap"),
                isSelected = true,
                onClick = {},
            ),
            TangemTabItemUM.Content(id = "top_gainers", label = stringReference("Top gainers"), onClick = {}),
            TangemTabItemUM.Content(id = "top_losers", label = stringReference("Top losers"), onClick = {}),
        ),
        onVisibleItemsChange = {},
        list = RwaListUM.Content(
            items = persistentListOf(
                RwaListItemUM(
                    row = TangemTokenRowMarket.State.Content(
                        id = "gold",
                        icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = "")),
                        title = stringReference("Gold"),
                        ticker = stringReference("XAU"),
                        position = stringReference("1"),
                        capitalization = stringReference("1.196 T"),
                        price = stringReference("$4,066.86"),
                        priceChange = TangemPriceChange.State(
                            value = stringReference("2.08%"),
                            direction = TangemPriceChange.Direction.Up,
                        ),
                        onClick = {},
                    ),
                    chartData = sparkline,
                    chartType = MarketChartLook.Type.Growing,
                ),
            ),
            loadMore = {},
        ),
        appliedSelection = CategorySelection(
            category = CategorySelection.Entry(id = "rwa_categories", displayName = "Stocks", code = "xstocks"),
        ),
        regionRestriction = TangemMessageBanner.State(
            title = resourceReference(R.string.markets_real_assets_region_restriction_title),
            description = resourceReference(
                id = R.string.markets_real_assets_region_restriction_description_region,
                formatArgs = wrappedList("United States"),
            ),
            variant = TangemMessageBanner.Variant.Warning,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_info_20),
        ),
    )
}

private fun previewFeaturedAsset(id: String, name: String, ticker: String, price: String): RwaFeaturedAssetUM {
    return RwaFeaturedAssetUM(
        id = id,
        iconUrl = "",
        indicator = null,
        name = stringReference(name),
        ticker = stringReference(ticker),
        price = stringReference(price),
        priceChange = TangemPriceChange.State(
            value = stringReference("2,08%"),
            direction = TangemPriceChange.Direction.Down,
        ),
        onClick = {},
    )
}
// endregion