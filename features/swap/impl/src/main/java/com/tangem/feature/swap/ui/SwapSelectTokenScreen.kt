package com.tangem.feature.swap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerW2
import com.tangem.core.ui.components.appbar.ExpandableSearchView
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.tokenlist.PortfolioListItem
import com.tangem.core.ui.components.tokenlist.PortfolioTokensListItem
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.TokenToSelectState
import com.tangem.feature.swap.models.isEmptyState
import com.tangem.feature.swap.models.isNotFoundState
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.ui.market.swapMarketsListItems
import com.tangem.feature.swap.ui.preview.SwapSelectTokenPreviewProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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

        tokensToSelectItems(state.availableTokens, state.onTokenSelected)

        item { SpacerH12() }

        tokensToSelectItems(state.unavailableTokens, state.onTokenSelected)
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
            assetsTitle(count = state.tokensListData.totalTokensCount)
        }

        tokensListItems(
            tokensListData = state.tokensListData,
            isBalanceHidden = state.isBalanceHidden,
        )

        if (state.tokensListData is TokenListUMData.EmptyList) {
            item { SpacerH12() }
        } else {
            item { SpacerH32() }
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

private fun LazyListScope.assetsTitle(count: Int) {
    item(key = "assets_title") {
        Text(
            text = buildAnnotatedString {
                append(stringResourceSafe(R.string.swap_your_assets_title))
                withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
                    append(" $count")
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
            tokensListData.tokensList.forEach { item ->
                portfolioTokensList(
                    portfolio = item,
                    isBalanceHidden = isBalanceHidden,
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

internal fun LazyListScope.portfolioTokensList(portfolio: TokensListItemUM.Portfolio, isBalanceHidden: Boolean) {
    val tokens = portfolio.tokens
    val isExpanded = portfolio.isExpanded

    portfolioItem(
        portfolio = portfolio,
        modifier = Modifier.padding(top = 8.dp),
        isBalanceHidden = isBalanceHidden,
    )
    if (!isExpanded) return
    itemsIndexed(
        items = tokens,
        key = { _, item -> item.id },
        contentType = { _, item -> item::class.java },
        itemContent = { tokenIndex, token ->
            val indexWithHeader = tokenIndex.inc()
            PortfolioTokensListItem(
                state = token,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .animateItem()
                    .roundedShapeItemDecoration(
                        currentIndex = indexWithHeader,
                        lastIndex = tokens.lastIndex.inc(),
                        backgroundColor = TangemTheme.colors.background.primary,
                    )
                    .conditional(tokenIndex == tokens.lastIndex) {
                        Modifier.padding(bottom = 8.dp)
                    },
            )
        },
    )
}

private fun LazyListScope.portfolioItem(
    portfolio: TokensListItemUM.Portfolio,
    modifier: Modifier,
    isBalanceHidden: Boolean,
) {
    item(
        key = "account-${portfolio.id}",
        contentType = "account",
    ) {
        PortfolioListItem(
            state = portfolio,
            isBalanceHidden = isBalanceHidden,
            modifier = Modifier
                .animateItem()
                .roundedShapeItemDecoration(
                    currentIndex = 0,
                    lastIndex = portfolio.tokens.lastIndex.inc(),
                    backgroundColor = TangemTheme.colors.background.primary,
                )
                .then(modifier),
        )
    }
}

private fun LazyListScope.tokensToSelectItems(
    items: ImmutableList<TokenToSelectState>,
    onTokenClick: (String) -> Unit,
) {
    itemsIndexed(items = items) { index, item ->
        when (item) {
            is TokenToSelectState.Title -> {
                TitleHeader(
                    item = item,
                    modifier = Modifier.roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = items.lastIndex,
                    ),
                )
            }
            is TokenToSelectState.TokenToSelect -> {
                TokenItem(
                    token = item,
                    modifier = Modifier
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            lastIndex = items.lastIndex,
                        )
                        .background(TangemTheme.colors.background.action),
                    onTokenClick = {
                        onTokenClick(item.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun TitleHeader(item: TokenToSelectState.Title, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TangemTheme.colors.background.action),
    ) {
        Text(
            text = item.title.resolveReference().uppercase(),
            style = TangemTheme.typography.overline,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .padding(
                    top = TangemTheme.dimens.spacing16,
                    start = TangemTheme.dimens.spacing16,
                ),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun TokenItem(
    token: TokenToSelectState.TokenToSelect,
    onTokenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size72)
            .clickable(
                enabled = token.isAvailable,
                onClick = onTokenClick,
            )
            .padding(
                vertical = TangemTheme.dimens.spacing14,
                horizontal = TangemTheme.dimens.spacing16,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurrencyIcon(
            state = token.tokenIcon,
            shouldDisplayNetwork = true,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .padding(start = TangemTheme.dimens.spacing12),
        ) {
            EllipsisText(
                text = token.name,
                style = TangemTheme.typography.subtitle1,
                color = if (token.isAvailable) {
                    TangemTheme.colors.text.primary1
                } else {
                    TangemTheme.colors.text.tertiary
                },
            )
            SpacerW2()
            EllipsisText(
                text = token.symbol,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }

        if (token.addedTokenBalanceData != null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing8),
            ) {
                Text(
                    text = token.addedTokenBalanceData.amountEquivalent.orEmpty().orMaskWithStars(
                        maskWithStars = token.addedTokenBalanceData.isBalanceHidden &&
                            !token.addedTokenBalanceData.amountEquivalent.isNullOrEmpty(),
                    ),
                    style = TangemTheme.typography.subtitle1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    color = if (token.isAvailable) {
                        TangemTheme.colors.text.primary1
                    } else {
                        TangemTheme.colors.text.tertiary
                    },
                )
                SpacerW2()
                Text(
                    text = token.addedTokenBalanceData.amount.orEmpty().orMaskWithStars(
                        maskWithStars = token.addedTokenBalanceData.isBalanceHidden &&
                            !token.addedTokenBalanceData.amount.isNullOrEmpty(),
                    ),
                    maxLines = 1,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

private class SwapSelectTokenScreenPreviewProvider : PreviewParameterProvider<SwapSelectTokenStateHolder> {
    override val values: Sequence<SwapSelectTokenStateHolder> = sequenceOf(
        // Content state with tokens and markets
        SwapSelectTokenPreviewProvider().provideSwapSelectTokenState(),
        // Empty state
        SwapSelectTokenStateHolder(
            availableTokens = persistentListOf(),
            unavailableTokens = persistentListOf(),
            tokensListData = TokenListUMData.EmptyList,
            marketsState = null,
            isAfterSearch = false,
            isBalanceHidden = false,
            onSearchEntered = {},
            onTokenSelected = {},
        ),
        // Not found state
        SwapSelectTokenStateHolder(
            availableTokens = persistentListOf(),
            unavailableTokens = persistentListOf(),
            tokensListData = TokenListUMData.EmptyList,
            marketsState = null,
            isAfterSearch = true,
            isBalanceHidden = false,
            onSearchEntered = {},
            onTokenSelected = {},
        ),
    )
}

@Preview
@Composable
private fun TokenScreenPreview(
    @PreviewParameter(SwapSelectTokenScreenPreviewProvider::class)
    state: SwapSelectTokenStateHolder,
) {
    TangemThemePreview {
        SwapSelectTokenScreen(
            state = state,
            onBack = {},
        )
    }
}