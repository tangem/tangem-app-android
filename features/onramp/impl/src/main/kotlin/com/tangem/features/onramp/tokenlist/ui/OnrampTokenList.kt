package com.tangem.features.onramp.tokenlist.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.TangemSearchBarDefaults
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.tokenlist.PortfolioListItem
import com.tangem.core.ui.components.tokenlist.PortfolioTokensListItem
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.row.header.TangemHeaderRow
import com.tangem.core.ui.ds2.accountrow.Section
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow as TangemTokenRowV2
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.features.onramp.tokenlist.entity.OnrampTokenGroupUM
import com.tangem.features.onramp.tokenlist.entity.OnrampTokenRowUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMData
import com.tangem.features.onramp.tokenlist.ui.preview.PreviewTokenListUMProvider
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList

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

    if (state.groups.isNotEmpty()) {
        state.groups.forEach { group -> tokenGroup(group = group, isBalanceHidden = state.isBalanceHidden) }
    } else {
        tokensListData(state = state)
    }
}

private fun LazyListScope.tokenGroup(group: OnrampTokenGroupUM, isBalanceHidden: Boolean) {
    val lastIndex = group.rows.size
    item(key = "group-${group.id}", contentType = "onramp-group-header") {
        val modifier = Modifier.roundedShapeItemDecoration(
            currentIndex = 0,
            lastIndex = lastIndex,
            backgroundColor = TangemTheme.colors3.bg.secondary,
        )
        when (val header = group.header) {
            is OnrampTokenGroupUM.Header.Section -> TangemAccountRow.Section(title = header.title, modifier = modifier)
            is OnrampTokenGroupUM.Header.Account -> TangemHeaderRow(
                headerRowUM = header.headerRowUM,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
        }
    }
    itemsIndexed(
        items = group.rows,
        key = { _, row -> "${group.id}-${row.id}" },
        contentType = { _, row -> row::class.java },
    ) { index, row ->
        val modifier = Modifier
            .roundedShapeItemDecoration(
                currentIndex = index + 1,
                lastIndex = lastIndex,
                backgroundColor = TangemTheme.colors3.bg.secondary,
            )
            .testTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
            .semantics { lazyListItemPosition = index }
        when (row) {
            is OnrampTokenRowUM.Available -> TangemTokenRowV2(
                state = row.state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
            is OnrampTokenRowUM.Unavailable -> UnavailableTokenRow(
                row = row,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun UnavailableTokenRow(
    row: OnrampTokenRowUM.Unavailable,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { TangemTokenIcon(state = row.icon, size = TangemTokenIcon.Size.X40) },
        titleSlot = { DimmedText(text = row.title, style = TangemTheme.typography3.body.medium) },
        subtitleSlot = { DimmedText(text = row.reason, style = TangemTheme.typography3.caption.medium) },
        valueSlot = row.fiatBalance?.let { fiat ->
            { DimmedText(text = fiat.orMaskWithStars(isBalanceHidden), style = TangemTheme.typography3.body.medium) }
        },
        subvalueSlot = row.cryptoBalance?.let { crypto ->
            {
                DimmedText(
                    text = crypto.orMaskWithStars(isBalanceHidden),
                    style = TangemTheme.typography3.caption.medium,
                )
            }
        },
    )
}

@Composable
private fun DimmedText(text: TextReference, style: TextStyle) {
    Text(
        text = text.resolveReference(),
        style = style,
        color = TangemTheme.colors3.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
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
                            containerColor = TangemTheme.colors3.bg.secondary,
                        )
                    }
                    else -> {
                        Notification(config = warning.config, containerColor = TangemTheme.colors3.bg.disabled)
                    }
                }
            }
        }
    }
}

@RemoveWithToggle("AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED")
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
                        backgroundColor = TangemTheme.colors3.bg.secondary,
                    )
                    .testTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
                    .semantics { lazyListItemPosition = index },
            )
        },
    )
}

@RemoveWithToggle("AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED")
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
                        backgroundColor = TangemTheme.colors3.bg.secondary,
                    )
                    .conditional(tokenIndex == tokens.lastIndex) {
                        Modifier.padding(bottom = 8.dp)
                    },
            )
        },
    )
}

@RemoveWithToggle("AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED")
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
                    backgroundColor = TangemTheme.colors3.bg.secondary,
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
            modifier = Modifier.background(color = TangemTheme.colors3.bg.primary),
        ) {
            onrampTokenList(
                state = state,
            )
        }
    }
}