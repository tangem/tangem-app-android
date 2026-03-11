package com.tangem.features.onramp.tokenlist.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.TangemSearchBarDefaults
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.tokenlist.PortfolioListItem
import com.tangem.core.ui.components.tokenlist.PortfolioTokensListItem
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.features.onramp.swap.availablepairs.ui.swapMarketsListItems
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMData
import com.tangem.features.onramp.tokenlist.ui.preview.PreviewTokenListUMProvider
import kotlinx.collections.immutable.ImmutableList

/**
 * Token list for swap - automatically switches between normal and search mode with markets
 *
 * @param state state
 *
 */
internal fun LazyListScope.onrampSwapTokenList(state: TokenListUM) {
    if (state.marketsState != null) {
        onrampTokenListWithMarkets(state = state)
    } else {
        onrampTokenList(state = state)
    }
}

/**
 * Token list - normal mode (without markets)
 *
 * @param state state
 */
internal fun LazyListScope.onrampTokenList(state: TokenListUM) {
    val itemModifier = Modifier.padding(horizontal = 16.dp)

    warningOrSearchBar(state = state, itemModifier = itemModifier)

    tokensList(items = state.availableItems, isBalanceHidden = state.isBalanceHidden)

    tokensList(items = state.unavailableItems, isBalanceHidden = state.isBalanceHidden)

    tokensListData(state = state)
}

/**
 * Token list with markets - search mode
 *
 * @param state state
 */
private fun LazyListScope.onrampTokenListWithMarkets(state: TokenListUM) {
    val itemModifier = Modifier.padding(horizontal = 16.dp)

    warningOrSearchBar(state = state, itemModifier = itemModifier)

    // Check if user has any assets to show
    val hasAssets = state.availableItems.isNotEmpty() ||
        state.unavailableItems.isNotEmpty() ||
        state.tokensListData.totalTokensCount != 0

    if (hasAssets) {
        assetsTitle(
            count = state.tokensListData.totalTokensCount,
            showCount = state.marketsState?.shouldAssetsCount == true,
        )

        tokensList(items = state.availableItems, isBalanceHidden = state.isBalanceHidden)

        tokensList(items = state.unavailableItems, isBalanceHidden = state.isBalanceHidden)

        tokensListData(state = state)

        item { SpacerH32() }
    }

    state.marketsState?.let(::swapMarketsListItems)
}

private fun LazyListScope.warningOrSearchBar(state: TokenListUM, itemModifier: Modifier) {
    if (state.warning == null) {
        searchBarItem(searchBarUM = state.searchBarUM, modifier = itemModifier)
    } else {
        item("NotificationsKey") {
            AnimatedContent(
                targetState = state.warning,
                label = "",
                modifier = itemModifier,
            ) { warning ->
                when (warning) {
                    is NotificationUM.Warning.OnrampErrorNotification -> {
                        Notification(
                            config = warning.config,
                            containerColor = TangemTheme.colors.background.primary,
                        )
                    }
                    else -> {
                        Notification(config = warning.config, containerColor = TangemTheme.colors.button.disabled)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.tokensListData(state: TokenListUM) {
    when (val list = state.tokensListData) {
        is TokenListUMData.AccountList -> list.tokensList.forEach { item ->
            portfolioTokensList(
                portfolio = item,
                isBalanceHidden = state.isBalanceHidden,
            )
        }
        is TokenListUMData.TokenList -> {
            tokensList(
                items = list.tokensList,
                isBalanceHidden = state.isBalanceHidden,
            )
        }
        TokenListUMData.EmptyList -> Unit
    }
}

private fun LazyListScope.searchBarItem(searchBarUM: SearchBarUM, modifier: Modifier = Modifier) {
    item("SearchKey") {
        SearchBar(
            state = searchBarUM,
            colors = TangemSearchBarDefaults.secondaryTextFieldColors,
            modifier = modifier,
        )
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
        modifier = Modifier,
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

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenList(@PreviewParameter(PreviewTokenListUMProvider::class) state: TokenListUM) {
    TangemThemePreview {
        LazyColumn(
            modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        ) {
            onrampTokenList(
                state = state,
            )
        }
    }
}