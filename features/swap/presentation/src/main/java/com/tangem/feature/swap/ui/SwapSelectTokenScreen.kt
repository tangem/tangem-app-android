package com.tangem.feature.swap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.Strings
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.ExpandableSearchView
import com.tangem.core.ui.components.currency.tokenicon.TokenIcon
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun SwapSelectTokenScreen(state: SwapSelectTokenStateHolder, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier
            .systemBarsPadding()
            .background(color = TangemTheme.colors.background.secondary),
        content = { padding ->
            val modifier = Modifier.padding(padding)
            if (state.availableTokens.isEmpty() && state.unavailableTokens.isEmpty()) {
                EmptyTokensList(modifier)
            } else {
                ListOfTokens(state = state, modifier = modifier)
            }
        },
        topBar = {
            ExpandableSearchView(
                title = stringResource(R.string.swapping_token_list_title),
                onBackClick = onBack,
                placeholderSearchText = stringResource(id = R.string.common_search_tokens),
                onSearchChange = state.onSearchEntered,
                onSearchDisplayClose = { state.onSearchEntered("") },
                subtitle = stringResource(id = R.string.express_exchange_token_list_subtitle),
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
                text = stringResource(id = R.string.exchange_tokens_empty_tokens),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ListOfTokens(state: SwapSelectTokenStateHolder, modifier: Modifier = Modifier) {
    val screenBackgroundColor = TangemTheme.colors.background.secondary
    LazyColumn(
        modifier = modifier
            .background(color = screenBackgroundColor)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { SpacerH8() }

        tokensToSelectItems(state.availableTokens, state.onTokenSelected)

        item { SpacerH12() }

        tokensToSelectItems(state.unavailableTokens, state.onTokenSelected)

        item { SpacerH12() }
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
                enabled = token.available,
                onClick = onTokenClick,
            )
            .padding(
                vertical = TangemTheme.dimens.spacing14,
                horizontal = TangemTheme.dimens.spacing16,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenIcon(
            state = token.tokenIcon,
            shouldDisplayNetwork = true,
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = TangemTheme.dimens.spacing12),
        ) {
            Text(
                text = token.name,
                style = TangemTheme.typography.subtitle1,
                color = if (token.available) {
                    TangemTheme.colors.text.primary1
                } else {
                    TangemTheme.colors.text.tertiary
                },
            )
            SpacerW2()
            Text(
                text = token.symbol,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (token.addedTokenBalanceData != null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
            ) {
                Text(
                    text = if (token.addedTokenBalanceData.isBalanceHidden &&
                        !token.addedTokenBalanceData.amountEquivalent.isNullOrEmpty()
                    ) {
                        Strings.STARS
                    } else {
                        token.addedTokenBalanceData.amountEquivalent.orEmpty()
                    },
                    style = TangemTheme.typography.subtitle1,
                    color = if (token.available) {
                        TangemTheme.colors.text.primary1
                    } else {
                        TangemTheme.colors.text.tertiary
                    },
                )
                SpacerW2()
                Text(
                    text = if (token.addedTokenBalanceData.isBalanceHidden &&
                        !token.addedTokenBalanceData.amount.isNullOrEmpty()
                    ) {
                        Strings.STARS
                    } else {
                        token.addedTokenBalanceData.amount.orEmpty()
                    },
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

private val token = TokenToSelectState.TokenToSelect(
    tokenIcon = TokenIconState.CoinIcon(
        url = "",
        fallbackResId = 0,
        isGrayscale = false,
        showCustomBadge = false,
    ),
    id = "",
    name = "USDC",
    symbol = "USDC",
    addedTokenBalanceData = TokenBalanceData(
        amount = "15 000 $",
        amountEquivalent = "15 000 " +
            "USDT",
        isBalanceHidden = false,
    ),
)

private val title = TokenToSelectState.Title(
    title = stringReference("MY TOKENS"),
)

@Preview
@Composable
private fun TokenScreenPreview() {
    TangemTheme(isDark = false) {
        SwapSelectTokenScreen(
            state = SwapSelectTokenStateHolder(
                availableTokens = listOf(title, token, token, token).toImmutableList(),
                unavailableTokens = listOf(title, token, token, token).toImmutableList(),
                onSearchEntered = {},
                onTokenSelected = {},
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun EmptyTokensListPreview() {
    TangemTheme(isDark = false) {
        EmptyTokensList()
    }
}
