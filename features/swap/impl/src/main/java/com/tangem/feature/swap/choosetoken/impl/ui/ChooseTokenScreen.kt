package com.tangem.feature.swap.choosetoken.impl.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.tokens.portfolioTokensList
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.TangemSearchBarDefaults
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.token.AccountItemPreviewData
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.components.tokenlist.state.PortfolioItemContentUM
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.utils.TangemSharedTransitionLayout
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.core.ui.utils.rememberHideKeyboardNestedScrollConnection
import com.tangem.feature.swap.choosetoken.api.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.feature.swap.choosetoken.api.model.TokenListUMData
import com.tangem.feature.swap.choosetoken.api.model.WalletListUM
import com.tangem.feature.swap.choosetoken.api.model.WalletTabUM
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.ui.market.swapMarketsListItems
import com.tangem.feature.swap.ui.preview.SwapSelectTokenPreviewProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.random.Random

private const val LOAD_MORE_BUFFER = 25

private val ChooseTokenFullUM.isNotFoundState: Boolean
    get() {
        if (portfolioBlock == null) return false
        if (marketsBlock == null) return false
        return portfolioBlock.tokensListData.tokensList.isEmpty() &&
            portfolioBlock.isSearching &&
            marketsBlock !is SwapMarketState.Content &&
            marketsBlock !is SwapMarketState.Loading
    }

private val ChooseTokenFullUM.isEmptyState: Boolean
    get() {
        if (portfolioBlock == null) return false
        if (marketsBlock == null) return false
        return portfolioBlock.tokensListData.tokensList.isEmpty() &&
            !portfolioBlock.isSearching &&
            marketsBlock !is SwapMarketState.Content &&
            marketsBlock !is SwapMarketState.Loading
    }

@Composable
internal fun ChooseTokenScreen(state: ChooseTokenFullUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxSize()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBar(title = state.initialUM.screenTitle, onBackClick = state.initialUM.onCloseClick, Modifier)

        Content(
            state = state,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AppBar(title: TextReference, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    AppBarWithBackButton(
        text = title.resolveReference(),
        onBackClick = onBackClick,
        iconRes = com.tangem.common.ui.R.drawable.ic_back_24,
        modifier = modifier
            .statusBarsPadding()
            .height(TangemTheme.dimens.size56),
    )
}

@Composable
private fun Content(state: ChooseTokenFullUM, modifier: Modifier = Modifier) {
    val nestedScrollConnection = rememberHideKeyboardNestedScrollConnection()
    val lazyListState = rememberLazyListState()

    TangemSharedTransitionLayout(modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            state = lazyListState,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            item(key = "search_bar") {
                SearchBar(
                    modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                    state = state.initialUM.searchBar,
                    colors = TangemSearchBarDefaults.secondaryTextFieldColors,
                )
            }

            assetsTitle()

            if (state.portfolioBlock != null) {
                walletListItem(state.portfolioBlock.walletList)
                when {
                    state.isNotFoundState -> tokensNotFound()
                    state.isEmptyState -> emptyTokensList()
                    else -> {
                        tokensListItems(
                            tokensListData = state.portfolioBlock.tokensListData,
                            isBalanceHidden = state.portfolioBlock.isBalanceHidden,
                        )

                        if (state.marketsBlock != null) {
                            item("markets_title_spacer") { SpacerH(height = 20.dp) }
                            swapMarketsListItems(state.marketsBlock)
                        }
                    }
                }
            }
        }
    }
    if (state.marketsBlock != null && !state.isNotFoundState && !state.isEmptyState) {
        SetupMarketScrollTracker(state.marketsBlock, lazyListState)
    }
}

@Composable
private fun SetupMarketScrollTracker(marketsState: SwapMarketState, lazyListState: LazyListState) {
    if (marketsState !is SwapMarketState.Content) return
    val onLoadMore = remember(marketsState) {
        {
            marketsState.loadMore()
            true
        }
    }
    VisibleItemsTracker(
        lazyListState = lazyListState,
        marketState = marketsState,
    )

    InfiniteListHandler(
        listState = lazyListState,
        buffer = LOAD_MORE_BUFFER,
        triggerLoadMoreCheckOnItemsCountChange = true,
        onLoadMore = onLoadMore,
    )
}

@Composable
private fun VisibleItemsTracker(lazyListState: LazyListState, marketState: SwapMarketState.Content) {
    val visibleItems by remember {
        derivedStateOf {
            lazyListState.layoutInfo.visibleItemsInfo.mapNotNull { itemInfo ->
                marketState.items.find { it.getComposeKey() == itemInfo.key }?.id
            }
        }
    }

    LaunchedEffect(visibleItems) {
        marketState.visibleIdsChanged(visibleItems)
    }
}

private fun LazyListScope.assetsTitle() {
    item(key = "assets_title") {
        Text(
            text = stringResourceSafe(R.string.swap_your_assets_title),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = TangemTheme.dimens.spacing20,
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                ),
        )
    }
}

private fun LazyListScope.walletListItem(walletList: WalletListUM) {
    if (walletList.items.isEmpty()) return
    item("wallet_list") {
        LazyRow(
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(walletList.items) { um ->
                WalletTabItem(um)
            }
        }
    }
}

@Composable
private fun WalletTabItem(state: WalletTabUM, modifier: Modifier = Modifier) {
    val isSelected = state.isSelected
    val backgroundColor = if (isSelected) TangemTheme.colors.button.primary else TangemTheme.colors.button.secondary
    val buttonTextColor = if (isSelected) TangemTheme.colors.text.primary2 else TangemTheme.colors.text.primary1
    val countTextColor = if (isSelected) TangemTheme.colors.text.primary2 else TangemTheme.colors.text.secondary
    val countBackground = if (isSelected) {
        TangemTheme.colors.button.secondary.copy(alpha = 0.2f)
    } else {
        TangemTheme.colors.button.primary.copy(alpha = 0.1f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = state.onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.text.resolveReference(),
            color = buttonTextColor,
            style = TangemTheme.typography.button,
        )

        if (state.count != null) {
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(countBackground, shape = CircleShape)
                    .defaultMinSize(minWidth = 20.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.count.resolveReference(),
                    color = countTextColor,
                    style = TangemTheme.typography.caption1,
                )
            }
        }
    }
}

private fun LazyListScope.tokensListItems(tokensListData: TokenListUMData, isBalanceHidden: Boolean) {
    when (tokensListData) {
        is TokenListUMData.AccountList -> {
            tokensListData.tokensList.forEachIndexed { index, item ->
                portfolioTokensList(
                    portfolio = item,
                    portfolioIndex = index,
                    isBalanceHidden = isBalanceHidden,
                    testTag = BuyTokenScreenTestTags.LAZY_LIST_ITEM,
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

private fun LazyListScope.emptyTokensList(modifier: Modifier = Modifier) {
    item("EmptyTokensList") {
        Box(
            modifier = modifier
                .background(TangemTheme.colors.background.secondary)
                .fillParentMaxSize(),
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
}

private fun LazyListScope.tokensNotFound(modifier: Modifier = Modifier) {
    item("TokensNotFound") {
        Box(
            modifier = modifier
                .background(TangemTheme.colors.background.secondary)
                .fillParentMaxSize(),
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
}

@Preview
@Composable
private fun TokenScreenPreview(@PreviewParameter(ChooseTokenScreenPreviewProvider::class) state: ChooseTokenFullUM) {
    TangemThemePreview {
        ChooseTokenScreen(
            state = state,
            modifier = Modifier,
        )
    }
}

private val searchBar
    get() = SearchBarUM(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        onQueryChange = {},
        isActive = false,
        onActiveChange = {},
    )

private val tokenItem
    get() = TokenItemState.Content(
        id = Random.nextInt().toString(),
        iconState = CurrencyIconState.Locked,
        titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Bitcoin")),
        fiatAmountState = TokenItemState.FiatAmountState.Content(text = "12 368,14 \$"),
        subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "0,35853044 BTC"),
        subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
            price = "34 496,75 \$",
            priceChangePercent = "0,43 %",
            type = PriceChangeType.DOWN,
        ),
        onItemClick = {},
        onItemLongClick = {},
    )

private val tokens
    get() = persistentListOf(
        TokensListItemUM.GroupTitle(id = 111, text = stringReference("Network Bitcoin")),
        TokensListItemUM.Token(state = tokenItem),
        TokensListItemUM.GroupTitle(id = 222, text = stringReference("Network Ethereum")),
        TokensListItemUM.Token(state = tokenItem),
        TokensListItemUM.Token(state = tokenItem),
        TokensListItemUM.Token(state = tokenItem),
    )

private val accounts
    get() = persistentListOf(
        TokensListItemUM.Portfolio(
            content = PortfolioItemContentUM.Tokens(
                tokens = tokens.filterIsInstance<PortfolioTokensListItemUM>().toPersistentList(),
            ),
            isExpanded = false,
            isCollapsable = true,
            tokenItemUM = AccountItemPreviewData.accountItem.copy(iconState = AccountItemPreviewData.accountLetterIcon),
        ),
        TokensListItemUM.Portfolio(
            content = PortfolioItemContentUM.Tokens(
                tokens = tokens.filterIsInstance<PortfolioTokensListItemUM>().toPersistentList(),
            ),
            isExpanded = true,
            isCollapsable = true,
            tokenItemUM = AccountItemPreviewData.accountItem,
        ),
    )

private val wallets
    get() = persistentListOf(
        WalletTabUM(
            text = TextReference.Str(value = "Wallet 1"),
            isSelected = true,
            onClick = {},
            count = null,
        ),
        WalletTabUM(
            text = TextReference.Str(value = "Wallet 1"),
            isSelected = true,
            onClick = {},
            count = stringReference("3"),
        ),
        WalletTabUM(
            text = TextReference.Str(value = "Wallet 2"),
            isSelected = false,
            onClick = {},
            count = stringReference("333"),
        ),
        WalletTabUM(
            text = TextReference.Str(value = "Wallet 3"),
            isSelected = false,
            onClick = {},
            count = null,
        ),
    )

private val initialUM = ChooseTokenInitialUM(
    screenTitle = stringReference("Choose token"),
    onCloseClick = {},
    searchBar = searchBar,
)

private class ChooseTokenScreenPreviewProvider : PreviewParameterProvider<ChooseTokenFullUM> {
    override val values: Sequence<ChooseTokenFullUM> = sequenceOf(
        ChooseTokenFullUM(
            initialUM = initialUM,
            portfolioBlock = ChooseTokenPortfolioFullBlockUM(
                walletList = WalletListUM(wallets),
                isBalanceHidden = false,
                isSearching = false,
                tokensListData = TokenListUMData.AccountList(
                    tokensList = accounts,
                    accounts.size,
                ),
            ),
            marketsBlock = SwapSelectTokenPreviewProvider.defaultState.marketsState,
        ),
        ChooseTokenFullUM(
            initialUM = initialUM,
            portfolioBlock = ChooseTokenPortfolioFullBlockUM(
                walletList = WalletListUM(wallets),
                isBalanceHidden = false,
                isSearching = false,
                tokensListData = TokenListUMData.EmptyList,
            ),
            marketsBlock = null,
        ),
    )
}