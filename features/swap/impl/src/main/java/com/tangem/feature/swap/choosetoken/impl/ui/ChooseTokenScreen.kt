package com.tangem.feature.swap.choosetoken.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.tokens.portfolioTokensList
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
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
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.core.ui.utils.rememberHideKeyboardNestedScrollConnection
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.ui.market.swapMarketsListItems
import com.tangem.feature.swap.ui.preview.SwapSelectTokenPreviewProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.random.Random

private const val LOAD_MORE_BUFFER = 25

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        horizontalAlignment = Alignment.CenterHorizontally,
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

        if (state.contentUM != null) {
            walletListItem(state.contentUM.walletList)

            tokensListItems(
                tokensListData = state.contentUM.tokensListData,
                isBalanceHidden = state.contentUM.isBalanceHidden,
            )

            if (state.contentUM.marketsState != null) {
                item("markets_title_spacer") { SpacerH(height = 20.dp) }
                swapMarketsListItems(state.contentUM.marketsState)
            }
        }
    }
    if (state.contentUM?.marketsState != null) {
        SetupMarketScrollTracker(state.contentUM.marketsState, lazyListState)
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
        Row(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            walletList.items.forEach { um ->
                val colors = when (um.type) {
                    TangemButtonType.Primary -> TangemButtonsDefaults.primaryButtonColors
                    else -> TangemButtonsDefaults.secondaryButtonColors
                }
                TangemButton(
                    text = um.text?.resolveReference().orEmpty(),
                    icon = TangemButtonIconPosition.None,
                    size = TangemButtonSize.Action,
                    colors = colors,
                    showProgress = false,
                    onClick = um.onClick,
                    enabled = true,
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
        TangemButtonUM(
            text = TextReference.Str(value = "Wallet 1"),
            type = TangemButtonType.Primary,
            onClick = {},
        ),
        TangemButtonUM(
            text = TextReference.Str(value = "Wallet 2"),
            type = TangemButtonType.Secondary,
            onClick = {},
        ),
        TangemButtonUM(
            text = TextReference.Str(value = "Wallet 3"),
            type = TangemButtonType.Secondary,
            onClick = {},
        ),
    )

private class ChooseTokenScreenPreviewProvider : PreviewParameterProvider<ChooseTokenFullUM> {
    override val values: Sequence<ChooseTokenFullUM> = sequenceOf(
        ChooseTokenFullUM(
            initialUM = ChooseTokenInitialUM(
                screenTitle = stringReference("Choose token"),
                onCloseClick = {},
                searchBar = searchBar,
            ),
            contentUM = ChooseTokenUM(
                walletList = WalletListUM(wallets),
                isBalanceHidden = false,
                isSearching = false,
                tokensListData = TokenListUMData.AccountList(
                    tokensList = accounts,
                    accounts.size,
                ),
                marketsState = SwapSelectTokenPreviewProvider.defaultState.marketsState,
            ),
        ),
    )
}