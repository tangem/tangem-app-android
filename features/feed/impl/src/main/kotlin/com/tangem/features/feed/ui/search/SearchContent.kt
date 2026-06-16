package com.tangem.features.feed.ui.search

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.markets.MarketsListItem
import com.tangem.common.ui.markets.MarketsListItemPlaceholder
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.markets.tokenselector.GroupedUserAssetItem
import com.tangem.common.ui.markets.tokenselector.SingleUserAssetItem
import com.tangem.common.ui.markets.tokenselector.UserAssetItemUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.components.MarketBlock
import com.tangem.features.feed.ui.feed.state.MarketChartUM
import com.tangem.features.feed.ui.search.state.MarketSearchResultUM
import com.tangem.features.feed.ui.search.state.SearchCallbacks
import com.tangem.features.feed.ui.search.state.SearchContentUM
import com.tangem.features.feed.ui.search.state.TextHintItemUM
import kotlinx.collections.immutable.ImmutableList

private const val PLACEHOLDER_COUNT = 10
private const val LOAD_MORE_THRESHOLD = 5
private const val USER_ASSETS_LIMIT = 3

@Composable
internal fun SearchContent(
    content: SearchContentUM,
    topMarkets: MarketChartUM,
    searchCallbacks: SearchCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val lazyListState = rememberLazyListState()
    val background = LocalMainBottomSheetColor.current.value

    val contentStructureKey = when (content) {
        is SearchContentUM.InitialEmpty -> "empty"
        is SearchContentUM.History -> "history"
        is SearchContentUM.Results -> "results_${content.userAssets.isNotEmpty()}"
    }
    LaunchedEffect(contentStructureKey) {
        lazyListState.scrollToItem(0)
    }

    var isUserAssetsExpanded by rememberSaveable { mutableStateOf(false) }
    val shouldShowUserAssetsPortfolio = content is SearchContentUM.Results && content.userAssets.isNotEmpty()
    LaunchedEffect(shouldShowUserAssetsPortfolio) {
        if (!shouldShowUserAssetsPortfolio) {
            isUserAssetsExpanded = false
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(background) },
        contentPadding = PaddingValues(
            start = TangemTheme.dimens2.x4,
            end = TangemTheme.dimens2.x4,
            bottom = bottomBarHeight,
            top = contentPadding.calculateTopPadding(),
        ),
    ) {
        when (content) {
            is SearchContentUM.InitialEmpty -> Unit
            is SearchContentUM.History -> if (content.textHints.isEmpty() && content.recentTokens.isEmpty()) {
                topMarketsBlock(topMarkets, searchCallbacks)
            } else {
                searchHistoryItems(
                    history = content,
                    onClearAllClick = searchCallbacks.onClearHintsClick,
                    onHintClick = searchCallbacks.onTextHintClick,
                    onHistoryTokenClick = searchCallbacks.onHistoryTokenClick,
                )
            }
            is SearchContentUM.Results -> searchResultsItems(
                results = content,
                isUserAssetsExpanded = isUserAssetsExpanded,
                onUserAssetsExpandedChange = { isUserAssetsExpanded = it },
                onResultMarketTokenClick = searchCallbacks.onResultMarketTokenClick,
            )
        }
    }
    if (content is SearchContentUM.Results) {
        InfiniteListHandler(
            listState = lazyListState,
            buffer = LOAD_MORE_THRESHOLD,
            triggerLoadMoreCheckOnItemsCountChange = true,
            onLoadMore = remember(content.marketTokens) {
                {
                    searchCallbacks.onLoadMore()
                    true
                }
            },
        )
    }
}

private fun LazyListScope.topMarketsBlock(topMarkets: MarketChartUM, searchCallbacks: SearchCallbacks) {
    item(key = "top_markets") {
        MarketBlock(
            marketChart = topMarkets,
            onSeeAllClick = searchCallbacks.onTopMarketSeeAllClick,
            onItemClick = searchCallbacks.onTopMarketItemClick,
        )
    }
}

private fun LazyListScope.searchHistoryItems(
    history: SearchContentUM.History,
    onClearAllClick: (() -> Unit),
    onHintClick: (String) -> Unit,
    onHistoryTokenClick: (MarketsListItemUM) -> Unit,
) {
    if (!history.textHints.isEmpty() || !history.recentTokens.isEmpty()) {
        item(key = "recents") {
            SectionHeader(
                title = stringResourceSafe(R.string.markets_search_hint_header),
                onClearAllClick = onClearAllClick,
            )
        }
    }
    itemsIndexed(
        items = history.textHints,
        key = { _, item -> "hint_${item.text}" },
    ) { index, hint ->
        TextHintItem(hint = hint, onHintClick = { onHintClick(hint.text) })
        if (index < history.textHints.size - 1) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x2),
                color = TangemTheme.colors2.border.neutral.primary,
            )
        }
    }
    item {
        SpacerH(TangemTheme.dimens2.x2)
    }
    items(
        items = history.recentTokens,
        key = { "recent_${it.getComposeKey()}" },
    ) { token ->
        MarketsListItem(
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens2.x2)
                .background(
                    color = TangemTheme.colors2.surface.level3,
                    shape = RoundedCornerShape(TangemTheme.dimens2.x5),
                ),
            model = token,
            onClick = { onHistoryTokenClick(token) },
        )
    }
}

private fun LazyListScope.searchResultsItems(
    results: SearchContentUM.Results,
    isUserAssetsExpanded: Boolean,
    onUserAssetsExpandedChange: (Boolean) -> Unit,
    onResultMarketTokenClick: (MarketsListItemUM) -> Unit,
) {
    if (results.userAssets.isNotEmpty()) {
        item(key = "header_portfolio") {
            SectionHeader(title = stringResourceSafe(R.string.markets_search_portfolio_header))
        }
        userAssetsPortfolioItems(
            assets = results.userAssets,
            expanded = isUserAssetsExpanded,
            onExpandedChange = onUserAssetsExpandedChange,
        )
    }

    when (val market = results.marketTokens) {
        is MarketSearchResultUM.Empty -> Unit
        is MarketSearchResultUM.Content -> marketSearchResultItems(
            market = market,
            hasUserAssetsSection = results.userAssets.isNotEmpty(),
            onResultMarketTokenClick = onResultMarketTokenClick,
        )
        is MarketSearchResultUM.Loading -> marketSearchResultLoadingItems(
            hasUserAssetsSection = results.userAssets.isNotEmpty(),
        )
        is MarketSearchResultUM.NotFound -> marketSearchResultNotFoundItem()
    }
}

private fun LazyListScope.userAssetsPortfolioItems(
    assets: ImmutableList<UserAssetItemUM>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val shouldShowToggle = assets.size > USER_ASSETS_LIMIT
    val visibleCount = if (shouldShowToggle && !expanded) USER_ASSETS_LIMIT else assets.size

    items(
        count = visibleCount,
        key = { index -> "user_asset_${assets[index].id}" },
    ) { index ->
        Column(
            modifier = Modifier.animateItem(
                fadeInSpec = tween(durationMillis = 300),
                fadeOutSpec = tween(durationMillis = 250),
            ),
        ) {
            UserAssetItem(assets[index])
            SpacerH(TangemTheme.dimens2.x2)
        }
    }
    if (shouldShowToggle) {
        item(key = "user_assets_show_toggle") {
            ShowAllUserAssetsButton(
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(durationMillis = 300),
                    fadeOutSpec = tween(durationMillis = 250),
                ),
                isExpanded = expanded,
                onClick = { onExpandedChange(!expanded) },
            )
        }
    }
}

private fun LazyListScope.marketSearchResultItems(
    market: MarketSearchResultUM.Content,
    hasUserAssetsSection: Boolean,
    onResultMarketTokenClick: (MarketsListItemUM) -> Unit,
) {
    if (hasUserAssetsSection) {
        item(key = "spacer_between_sections") {
            SpacerH(TangemTheme.dimens2.x9)
        }
    }
    item(key = "header_market") {
        SectionHeader(title = stringResourceSafe(R.string.markets_common_title))
    }
    items(
        items = market.items,
        key = { "market_${it.getComposeKey()}" },
    ) { token ->
        MarketsListItem(
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens2.x2)
                .background(
                    color = TangemTheme.colors2.surface.level3,
                    shape = RoundedCornerShape(TangemTheme.dimens2.x5),
                ),
            model = token,
            onClick = { onResultMarketTokenClick(token) },
        )
    }
    if (market.shouldShowUnder100kNotification) {
        item(key = "show_tokens_under_100k") {
            ShowTokensUnder100kItem(
                onShowTokensClick = market.onShowUnder100kClick,
            )
        }
    }
}

private fun LazyListScope.marketSearchResultLoadingItems(hasUserAssetsSection: Boolean) {
    if (hasUserAssetsSection) {
        item(key = "spacer_between_sections") {
            SpacerH(TangemTheme.dimens2.x9)
        }
    }
    item(key = "header_market") {
        SectionHeader(title = stringResourceSafe(R.string.markets_common_title))
    }
    items(count = PLACEHOLDER_COUNT) {
        MarketsListItemPlaceholder()
    }
}

private fun LazyListScope.marketSearchResultNotFoundItem() {
    item(key = "not_found") {
        Box(
            modifier = Modifier.fillParentMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResourceSafe(R.string.common_no_results),
                style = TangemTheme.typography2.subheadlineMedium14,
                color = TangemTheme.colors2.text.neutral.tertiary,
            )
        }
    }
}

@Composable
private fun TextHintItem(hint: TextHintItemUM, onHintClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onHintClick)
            .padding(vertical = 22.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_search_default_24),
            contentDescription = null,
            tint = TangemTheme.colors2.graphic.neutral.primary,
            modifier = Modifier.size(TangemTheme.dimens2.x5),
        )
        SpacerW(TangemTheme.dimens2.x1)
        Text(
            modifier = Modifier.weight(1f),
            text = hint.text,
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        SpacerW(TangemTheme.dimens2.x2)
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_return_24),
            contentDescription = null,
            tint = TangemTheme.colors2.markers.iconGray,
            modifier = Modifier.size(TangemTheme.dimens2.x6),
        )
    }
}

@Composable
private fun UserAssetItem(asset: UserAssetItemUM) {
    when (asset) {
        is UserAssetItemUM.Single -> Box(
            modifier = Modifier.background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            ),
        ) {
            SingleUserAssetItem(item = asset, shouldUsePriceBlock = true)
        }
        is UserAssetItemUM.Grouped -> GroupedUserAssetItem(item = asset)
    }
}

@Composable
private fun ShowAllUserAssetsButton(isExpanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        TangemButton(
            buttonUM = TangemButtonUM(
                text = if (isExpanded) {
                    resourceReference(R.string.feed_search_show_less_user_assets)
                } else {
                    resourceReference(R.string.feed_search_show_all_user_assets)
                },
                tangemIconUM = TangemIconUM.Icon(
                    iconRes = if (isExpanded) R.drawable.ic_chewron_up_20 else R.drawable.ic_chewron_down_20,
                ),
                iconPosition = TangemButtonIconPosition.End,
                onClick = onClick,
                type = TangemButtonType.Secondary,
                size = TangemButtonSize.X7,
                shape = TangemButtonShape.Rounded,
            ),
        )
    }
}

@Composable
private fun ShowTokensUnder100kItem(onShowTokensClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                top = TangemTheme.dimens2.x9,
                bottom = TangemTheme.dimens2.x3,
                start = TangemTheme.dimens2.x10,
                end = TangemTheme.dimens2.x10,
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.markets_search_see_tokens_under_100k),
            style = TangemTheme.typography2.subheadlineMedium14,
            color = TangemTheme.colors2.text.neutral.secondary,
        )
        TangemButton(
            buttonUM = TangemButtonUM(
                text = resourceReference(R.string.markets_search_show_tokens),
                onClick = onShowTokensClick,
                type = TangemButtonType.Secondary,
                size = TangemButtonSize.X8,
                shape = TangemButtonShape.Rounded,
            ),
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier, onClearAllClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .padding(
                vertical = TangemTheme.dimens2.x3,
                horizontal = TangemTheme.dimens2.x2,
            )
            .padding(bottom = TangemTheme.dimens2.x3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = TangemTheme.typography2.headingSemibold20,
            color = TangemTheme.colors2.text.neutral.primary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )

        if (onClearAllClick != null) {
            Text(
                modifier = Modifier.clickableSingle(onClick = onClearAllClick),
                text = stringResourceSafe(R.string.markets_search_clear_all_hints),
                style = TangemTheme.typography2.bodySemibold16,
                color = TangemTheme.colors2.text.neutral.primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}