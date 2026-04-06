package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.appbar.ExpandableSearchView
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.tokenlist.PortfolioListItem
import com.tangem.core.ui.components.tokenlist.PortfolioTokensListItem
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.isEmptyState
import com.tangem.feature.swap.models.isNotFoundState
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.ui.market.swapMarketsListItems
import com.tangem.feature.swap.ui.preview.SwapSelectTokenPreviewProvider
import kotlinx.collections.immutable.ImmutableList

private const val LOAD_MORE_BUFFER = 25

@Composable
internal fun SwapSelectTokenScreen(state: SwapSelectTokenStateHolder, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier
            .systemBarsPadding()
            .background(color = TangemTheme.colors.background.secondary),
        content = { padding ->
            val modifier = Modifier.padding(padding)
            when {
                state.isNotFoundState -> TokensNotFound(modifier)
                state.isEmptyState -> EmptyTokensList(modifier)
                state.marketsState != null -> ListOfTokensWithMarkets(
                    state = state,
                    marketsState = state.marketsState,
                    modifier = modifier,
                )
                else -> ListOfTokens(state = state, modifier = modifier)
            }
        },
        topBar = {
            ExpandableSearchView(
                title = stringResourceSafe(R.string.common_choose_token),
                onBackClick = onBack,
                placeholderSearchText = stringResourceSafe(id = R.string.common_search_tokens),
                onSearchChange = state.onSearchEntered,
                onSearchDisplayClose = { state.onSearchEntered("") },
                subtitle = stringResourceSafe(id = R.string.express_exchange_token_list_subtitle),
            )
        },
    )
}

@Composable
private fun EmptyTokensList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary)
            .fillMaxSize(),
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Image(
                modifier = Modifier
                    .size(TangemTheme.dimens.size64)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.ic_no_token_44),
                colorFilter = ColorFilter.tint(TangemTheme.colors.icon.inactive),
                contentDescription = null,
            )
            Text(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing16)
                    .padding(horizontal = TangemTheme.dimens.spacing30)
                    .align(Alignment.CenterHorizontally),
                text = stringResourceSafe(id = R.string.exchange_tokens_empty_tokens),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TokensNotFound(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary)
            .fillMaxSize(),
    ) {
        Text(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing32)
                .padding(horizontal = TangemTheme.dimens.spacing30)
                .align(Alignment.TopCenter),
            text = stringResourceSafe(id = R.string.express_token_list_empty_search),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ListOfTokens(state: SwapSelectTokenStateHolder, modifier: Modifier = Modifier) {
    val screenBackgroundColor = TangemTheme.colors.background.secondary

    LazyColumn(
        modifier = modifier
            .background(color = screenBackgroundColor)
            .fillMaxSize()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        tokensListItems(
            tokensListData = state.tokensListData,
            isBalanceHidden = state.isBalanceHidden,
        )
    }
}

@Composable
private fun ListOfTokensWithMarkets(
    state: SwapSelectTokenStateHolder,
    marketsState: SwapMarketState,
    modifier: Modifier = Modifier,
) {
    val screenBackgroundColor = TangemTheme.colors.background.secondary
    val lazyListState = rememberLazyListState()

    LazyColumn(
        modifier = modifier
            .background(color = screenBackgroundColor)
            .fillMaxSize()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = lazyListState,
    ) {
        if (state.tokensListData !is TokenListUMData.EmptyList) {
            assetsTitle(count = state.tokensListData.totalTokensCount, showCount = marketsState.shouldAssetsCount)
        }

        tokensListItems(
            tokensListData = state.tokensListData,
            isBalanceHidden = state.isBalanceHidden,
        )

        item("spacer_before_markets") {
            SpacerH(32.dp)
        }

        swapMarketsListItems(marketsState)
    }

    (marketsState as? SwapMarketState.Content)?.let { content ->
        VisibleItemsTracker(
            lazyListState = lazyListState,
            marketState = content,
        )

        InfiniteListHandler(
            listState = lazyListState,
            buffer = LOAD_MORE_BUFFER,
            triggerLoadMoreCheckOnItemsCountChange = true,
            onLoadMore = remember(content) {
                {
                    content.loadMore()
                    true
                }
            },
        )
    }
}

@Composable
private fun VisibleItemsTracker(lazyListState: LazyListState, marketState: SwapMarketState.Content) {
    val visibleItems by remember {
        derivedStateOf {
            lazyListState.layoutInfo.visibleItemsInfo
                .mapNotNull { itemInfo ->
                    marketState.items.find { it.getComposeKey() == itemInfo.key }?.id
                }
        }
    }

    LaunchedEffect(visibleItems) {
        marketState.visibleIdsChanged(visibleItems)
    }
}

private fun LazyListScope.assetsTitle(count: Int, showCount: Boolean) {
    item(key = "assets_title") {
        Text(
            text = buildAnnotatedString {
                append(stringResourceSafe(R.string.swap_your_assets_title))
                if (showCount) {
                    withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
                        append(" $count")
                    }
                }
            },
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    top = TangemTheme.dimens.spacing12,
                ),
        )
    }
}

private fun LazyListScope.tokensListItems(tokensListData: TokenListUMData, isBalanceHidden: Boolean) {
    when (tokensListData) {
        is TokenListUMData.AccountList -> {
            tokensListData.tokensList.forEachIndexed { index, item ->
                portfolioTokensList(
                    portfolio = item,
                    isBalanceHidden = isBalanceHidden,
                    portfolioIndex = index,
                    modifier = Modifier,
                )
            }
        }
        is TokenListUMData.TokenList -> {
            tokensList(
                items = tokensListData.tokensList,
                isBalanceHidden = isBalanceHidden,
            )
        }
        TokenListUMData.EmptyList -> Unit
    }
}

private fun LazyListScope.tokensList(items: ImmutableList<TokensListItemUM>, isBalanceHidden: Boolean) {
    itemsIndexed(
        items = items,
        key = { _, item -> item.id },
        contentType = { _, item -> item::class.java },
        itemContent = { index, item ->
            TokenListItem(
                state = item,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = items.lastIndex,
                        backgroundColor = TangemTheme.colors.background.primary,
                    )
                    .testTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
                    .semantics { lazyListItemPosition = index },
            )
        },
    )
}

internal fun LazyListScope.portfolioTokensList(
    portfolio: TokensListItemUM.Portfolio,
    modifier: Modifier,
    portfolioIndex: Int,
    isBalanceHidden: Boolean,
) {
    val tokens = portfolio.tokens
    val isExpanded = portfolio.isExpanded
    val lastIndex = tokens.lastIndex.inc()

    portfolioItem(
        portfolio = portfolio,
        modifier = modifier,
        portfolioIndex = portfolioIndex,
        isBalanceHidden = isBalanceHidden,
    )
    itemsIndexed(
        items = tokens,
        key = { _, item -> item.id.toString() + "-portfolio-${portfolio.id}" },
        contentType = { _, item -> item::class.java },
        itemContent = { tokenIndex, token ->
            val indexWithHeader = tokenIndex.inc()
            SlideInItemVisibility(
                currentIndex = tokenIndex,
                lastIndex = lastIndex,
                modifier = modifier
                    .testModifier(indexWithHeader)
                    .animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
                    .roundedShapeItemDecoration(
                        radius = TangemTheme.dimens.radius14,
                        currentIndex = indexWithHeader,
                        lastIndex = lastIndex,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
                visible = isExpanded,
            ) {
                val innerModifier = if (indexWithHeader == lastIndex) Modifier.padding(bottom = 8.dp) else Modifier
                PortfolioTokensListItem(
                    state = token,
                    isBalanceHidden = isBalanceHidden,
                    modifier = innerModifier,
                )
            }
        },
    )
}

@Suppress("MagicNumber")
private fun LazyListScope.portfolioItem(
    portfolio: TokensListItemUM.Portfolio,
    modifier: Modifier,
    portfolioIndex: Int,
    isBalanceHidden: Boolean,
) {
    val tokens = portfolio.tokens
    val isExpanded = portfolio.isExpanded
    val lastIndex = when {
        isExpanded && tokens.isEmpty() -> 1
        isExpanded -> tokens.lastIndex.inc()
        else -> 0
    }

    item(
        key = "account-${portfolio.id}",
        contentType = "account-content",
    ) {
        // Snap immediately on expand; on collapse, hold until all child items finish
        // their shrink animation, then snap to fully-rounded shape.
        val effectiveLastIndex by animateIntAsState(
            targetValue = lastIndex,
            animationSpec = if (lastIndex != 0) {
                snap()
            } else {
                snap(delayMillis = minOf(50 * tokens.lastIndex, 250) + 150)
            },
            label = "lastIndex",
        )

        PortfolioListItem(
            state = portfolio,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier
                .testModifier(portfolioIndex)
                .roundedShapeItemDecoration(
                    currentIndex = 0,
                    radius = TangemTheme.dimens.radius14,
                    lastIndex = effectiveLastIndex,
                    backgroundColor = TangemTheme.colors.background.primary,
                ),
        )
    }
}

private fun Modifier.testModifier(index: Int): Modifier = this
    .testTag(MainScreenTestTags.TOKEN_LIST_ITEM)
    .semantics { lazyListItemPosition = index }

@Suppress("MagicNumber")
@Composable
internal fun SlideInItemVisibility(
    visible: Boolean,
    currentIndex: Int,
    lastIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val maxDelay = 250
    val delayEnter = minOf(50 * currentIndex, maxDelay)
    val delayExit = minOf(50 * (lastIndex - currentIndex - 1), maxDelay)

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = expandVertically(
            tween(200, delayMillis = delayEnter, easing = LinearOutSlowInEasing),
            expandFrom = Alignment.Top,
        ) + fadeIn(tween(200, delayMillis = delayEnter, easing = LinearOutSlowInEasing)),
        exit = shrinkVertically(
            tween(150, delayMillis = delayExit, easing = FastOutLinearInEasing),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(tween(150, delayMillis = delayExit, easing = FastOutLinearInEasing)),
    ) {
        content()
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SwapSelectTokenScreen_Preview(
    @PreviewParameter(SwapSelectTokenScreenPreviewProvider::class) params: SwapSelectTokenStateHolder,
) {
    TangemThemePreview {
        SwapSelectTokenScreen(
            state = params,
            onBack = {},
        )
    }
}

private class SwapSelectTokenScreenPreviewProvider : PreviewParameterProvider<SwapSelectTokenStateHolder> {
    override val values: Sequence<SwapSelectTokenStateHolder>
        get() = sequenceOf(
            // Content state with tokens and markets
            SwapSelectTokenPreviewProvider.defaultState,
            // Empty state
            SwapSelectTokenStateHolder(
                tokensListData = TokenListUMData.EmptyList,
                marketsState = SwapMarketState.DefaultLoading,
                isAfterSearch = false,
                isBalanceHidden = false,
                onSearchEntered = {},
            ),
            // Not found state
            SwapSelectTokenStateHolder(
                tokensListData = TokenListUMData.EmptyList,
                marketsState = SwapMarketState.SearchLoading,
                isAfterSearch = true,
                isBalanceHidden = false,
                onSearchEntered = {},
            ),
        )
}
// endregion